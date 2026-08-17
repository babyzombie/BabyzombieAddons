package top.babyzombie.addons.module.fishing;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import top.babyzombie.addons.config.FishingConfig.CameraAspectRatio;
import top.babyzombie.addons.config.FishingConfig.CameraYawMode;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.event.AfterWorldRenderEvents;
import top.babyzombie.addons.util.render.SecondCameraRenderer;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/// 钓鱼浮标第二相机。
/// <p>
/// 在 GameRenderer 世界渲染完成之后,把主渲染目标临时换成独立的小目标,
/// 用鱼漂上方 40° 俯视的临时相机( Marker 实体)重跑 extract + renderLevel,
/// 得到鱼漂特写画面;
/// 在 HUD 把画面贴到屏幕。
public final class FishingCameraModule {

    /// 特写画面高度(物理像素),宽度按窗口比例,避免投影纵横比拉伸
    private static final int FEED_HEIGHT = 256;
    /// 帧率限制(渲染时间戳):secondCameraFrameRate 配置(帧/秒)
    private static long lastRenderMillis;
    /// 第二相机视锥远平面(格):限制区块挑选范围,减少提交量(视距配置,直接以格为单位)
    public static float secondCameraDepthFar() {
        return ModConfigManager.get().fishing.fishingCamera.viewDistance;
    }
    /// marker 相对浮漂的抬升(格):浮漂落地时 marker 会陷入方块内,
    /// 原版第三人称的射线避让从方块内开始检测会把相机压到贴脸,抬高后脱离方块。
    /// 保持低位:marker 高会让相机绕"浮标上方的空气"转、浮标偏离画面中心,
    /// 镜头高度由俯仰/距离配置控制(相机 = marker 斜上方)
    private static final double MARKER_LIFT = 0.3;

    private static @Nullable TextureTarget feedTarget;
    /// 本帧是否成功捕获,供 HUD 判断是否绘制
    private static boolean feedReady;
    /// 浮漂最后位置(浮漂消失后 linger 期间继续用此位置渲染)
    private static @Nullable Vec3 lastBobberPos;
    /// 最后看到浮漂的世界(换区/换世界后 linger 失效)
    private static @Nullable ClientLevel lastBobberLevel;
    /// 最后看到浮漂的游戏 tick(计时用)
    private static long lastBobberGameTime;

    private FishingCameraModule() {}

    public static void init() {
        // 捕获时机:AfterWorldRenderEvents(renderLevel 完全返回后,由 mixin 派发)。
        // 第二相机模块统一注册本事件,不需要各自写 mixin;
        // SecondCameraRenderer.capture 内部有防递归(重跑的 renderLevel 不会再触发本事件)
        AfterWorldRenderEvents.register(FishingCameraModule::capture);
        // 特写画面作为 HUD 元素注册:渲染在 HUD 层(原版机制,设置界面等
        // screen 打开时自动被遮挡,不需要手动判断)
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "fishing_camera"),
                (graphics, tickCounter) -> {
            if (!feedReady || feedTarget == null) return;
            if (!HudManager.shouldShow("FishingCamera")) return;
            drawHud(graphics);
        });
        // 捕获由 mixin 驱动;这里注册"Hypixel 上强制开启仅大厅/SkyBlock 限制"
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var cfg = ModConfigManager.get().fishing.fishingCamera;
            if (cfg.onlyLobbyOrSkyblock) return;
            if (HypixelLocationTracker.getInstance().isOnHypixel()) {
                cfg.onlyLobbyOrSkyblock = true;
            }
        });
    }

    /// 世界渲染完成后调用:渲染第二相机画面进 feedTarget。
    public static void capture(DeltaTracker realDelta) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (mc.level == null || player == null) {
            feedReady = false;
            return;
        }
        var bobber = player.fishing;
        var cfg = ModConfigManager.get().fishing.fishingCamera;
        // —— 浮漂已消失:linger 配置时长内继续用最后位置渲染(0 = 立即关闭) ——
        Vec3 bobberPos;
        if (bobber == null) {
            if (cfg.lingerTicks <= 0 || lastBobberLevel != mc.level || lastBobberPos == null
                    || mc.level.getGameTime() - lastBobberGameTime >= cfg.lingerTicks) {
                feedReady = false;
                return;
            }
            bobberPos = lastBobberPos;
        } else {
            lastBobberPos = bobber.position();
            lastBobberLevel = mc.level;
            lastBobberGameTime = mc.level.getGameTime();
            bobberPos = lastBobberPos;
        }
        // —— 配置条件(不满足时清掉残留画面) ——
        var loc = HypixelLocationTracker.getInstance();
        if (!cfg.enabled
                || (cfg.onlyLobbyOrSkyblock && loc.isOnHypixel()
                    && loc.getLobbyName() == null && !loc.isInSkyblock())
                || (cfg.disabledInKuudra && loc.isInKuudra())
                || (cfg.disabledInDungeon && loc.isInDungeon())) {
            feedReady = false;
            return;
        }
        // 帧率限制(按秒):距离上次渲染不足 1/fps 秒则跳过,保留上一帧画面(feedReady 不清)
        long now = Util.getMillis();
        if (now - lastRenderMillis < 1000L / Math.max(1, cfg.frameRate)) return;
        lastRenderMillis = now;

        var gameRenderer = mc.gameRenderer;
        var camera = gameRenderer.getMainCamera();

        // —— 懒创建 feed target(宽高按配置的画面比例) ——
        var ratio = cfg.aspectRatio == null ? CameraAspectRatio.R2_1 : cfg.aspectRatio;
        int feedWidth = Math.max(1, FEED_HEIGHT * ratio.w / ratio.h);
        if (feedTarget == null || feedTarget.width != feedWidth || feedTarget.height != FEED_HEIGHT) {
            if (feedTarget != null) feedTarget.destroyBuffers();
            feedTarget = new TextureTarget("bza_fishing_feed", feedWidth, FEED_HEIGHT, true);
        }

        // 相机朝向按配置(yawMode),俯视配置的 pitch;
        // 原版第三人称把相机放到 marker 斜上方 distance 格(射线避让方块),浮标落在画面中心。
        Vec3 playerPos = player.position();
        double toPlayerX = playerPos.x - bobberPos.x;
        double toPlayerZ = playerPos.z - bobberPos.z;
        // 兼容旧配置:枚举值可能已删除(yawMode = null),兜底为固定角度
        var yawMode = cfg.yawMode == null ? CameraYawMode.FIXED : cfg.yawMode;
        float yaw = switch (yawMode) {
            case FIXED -> 0.0F;
            case FRONT -> player.getYRot();
            case BACK -> player.getYRot() + 180.0F;
            case LEFT -> player.getYRot() - 90.0F;
            case RIGHT -> player.getYRot() + 90.0F;
        };
        // 偏航偏移 + 动态旋转只在固定角度生效
        if (yawMode == CameraYawMode.FIXED) {
            if (cfg.yawSpinSpeed > 0) {
                // 动态旋转:相机绕鱼漂持续旋转,速度(度/秒),0 = 关闭。
                // 用客户端毫秒(每帧平滑);gameTime 是服务器 tick(20Hz),旋转会一顿一顿
                yaw += (float) ((Util.getMillis() % 100000L) / 1000.0 * cfg.yawSpinSpeed) % 360.0F;
            }
            yaw += cfg.yawOffset;
        }
        // —— 第二相机渲染(util):渲染到 feedTarget,内部完整恢复主画面状态 ——
        feedReady = SecondCameraRenderer.capture(realDelta, new SecondCameraRenderer.CaptureParams(
                bobberPos, MARKER_LIFT, yaw, cfg.pitch, cfg.distance, secondCameraDepthFar(), feedTarget));
    }

    /// HUD 提取阶段调用:把特写画面贴到 HUD 元素位置(支持编辑/缩放)。
    /// 画中画边框宽度(逻辑像素)
    private static final int FRAME_BORDER = 2;

    public static void drawHud(GuiGraphicsExtractor graphics) {
        if (!feedReady || feedTarget == null) return;
        if (!HudManager.shouldShow("FishingCamera")) return;
        var textureView = feedTarget.getColorTextureView();
        if (textureView == null) return;
        float s = HudManager.scale("FishingCamera");
        var cfg = ModConfigManager.get().fishing.fishingCamera;
        int dh = Math.max(1, Math.round(192 * s));
        // 显示比例按配置,从窗口比例纹理中裁切居中区域(渲染 target 是窗口尺寸,
        // mod 发光纹理按窗口尺寸缓存,比例设置通过 UV 裁切生效)
        var ratio = cfg.aspectRatio == null ? CameraAspectRatio.R2_1 : cfg.aspectRatio;
        float r = ratio.w / (float) ratio.h;
        int dw = Math.max(1, Math.round(dh * r));
        float texR = feedTarget.width / (float) Math.max(1, feedTarget.height);
        float u0 = 0.0F, v0 = 1.0F, u1 = 1.0F, v1 = 0.0F;
        if (r >= texR) {
            float vRange = texR / r;
            v0 = (1.0F + vRange) / 2.0F;
            v1 = (1.0F - vRange) / 2.0F;
        } else {
            float uRange = r / texR;
            u0 = (1.0F - uRange) / 2.0F;
            u1 = (1.0F + uRange) / 2.0F;
        }
        int x0 = HudManager.x("FishingCamera");
        int y0 = HudManager.y("FishingCamera");
        GuiRenderState guiRenderState = graphics.guiRenderState;
        // 边框(背景色,像小地图):先画稍大的矩形,画中画覆盖在上面,四周露出边框
        var borderColor = ModConfigManager.get().fishing.fishingCamera.borderColor.getEffectiveColourRGB();
        guiRenderState.addGuiElement(new ColoredRectangleRenderState(
                RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(),
                x0 - FRAME_BORDER, y0 - FRAME_BORDER, x0 + dw + FRAME_BORDER, y0 + dh + FRAME_BORDER,
                borderColor, borderColor, null));
        guiRenderState.addBlitToCurrentLayer(new BlitRenderState(
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                TextureSetup.singleTexture(textureView,
                        RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                new Matrix3x2f(), x0, y0, x0 + dw, y0 + dh,
                u0, v0, u1, v1, -1, null, null));
    }
}
