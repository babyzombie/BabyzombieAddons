package top.babyzombie.addons.module.kuudra;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import top.babyzombie.addons.config.FishingConfig.CameraAspectRatio;
import top.babyzombie.addons.config.FishingConfig.CameraYawMode;
import top.babyzombie.addons.config.KuudraConfig.PearlTrajectoryCfg;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.event.AfterWorldRenderEvents;
import top.babyzombie.addons.util.render.SecondCameraRenderer;

/**
 * 末影珍珠第二相机:手持珍珠/投出后特写预测落点(同精灵球落地镜头);
 * 开启跟随珍珠时锚点改为珍珠实时位置,相机朝向由珍珠速度方向接管
 * (相机在珍珠后方沿轨迹看它,俯仰/偏航配置被忽略)。
 */
public final class EnderPearlCamera {

    /// 特写画面高度(物理像素),宽度按配置的画面比例
    private static final int FEED_HEIGHT = 256;
    /// 落点在地面,marker 抬升防止相机陷入方块(跟随珍珠时珍珠在空中,不需要)
    private static final double MARKER_LIFT = 1;
    /// 帧率限制(渲染时间戳)
    private static long lastRenderMillis;

    private static @Nullable TextureTarget feedTarget;
    /// 本帧是否成功捕获,供 HUD 判断是否绘制
    private static boolean feedReady;
    /// 本帧预测落地秒数(画中画左上角显示);无预测时为 -1
    private static float landingTimeSeconds = -1.0f;

    private EnderPearlCamera() {
    }

    public static void init() {
        // 捕获时机与精灵球相机相同(renderLevel 完全返回后)
        AfterWorldRenderEvents.register(EnderPearlCamera::capture);
        // HUD 元素:渲染在 HUD 层,设置界面等 screen 打开时自动被遮挡
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "pearl_camera"),
                (graphics, tickCounter) -> {
            if (!feedReady || feedTarget == null) return;
            if (!HudManager.shouldShow("EnderPearlCamera")) return;
            drawHud(graphics);
        });
    }

    private static void capture(DeltaTracker realDelta) {
        var mc = Minecraft.getInstance();
        var cfg = config();
        if (!cfg.enabled || !cfg.cameraEnabled || !EnderPearlTrajectory.phaseActive()
                || mc.level == null || mc.player == null) {
            feedReady = false;
            return;
        }
        // 手持珍珠(瞄准)或已有投出的珍珠时才显示;两者都无立即关闭
        if (!EnderPearlTrajectory.holdingPearl(mc.player) && EnderPearlTracker.trackedPearl() == null) {
            feedReady = false;
            return;
        }
        // 帧率限制(按秒):距离上次渲染不足 1/fps 秒则跳过,保留上一帧画面(feedReady 不清)
        long now = Util.getMillis();
        if (now - lastRenderMillis < 1000L / Math.max(1, cfg.pearlCameraFrameRate)) return;
        lastRenderMillis = now;
        // 实时预测(与轨迹线一致;预测失败时关闭)
        var prediction = EnderPearlTrajectory.currentPrediction();
        if (prediction == null) {
            feedReady = false;
            return;
        }
        landingTimeSeconds = prediction.ticks() / 20.0f;

        // 懒创建输出目标:窗口尺寸(与 mod 发光纹理按窗口尺寸缓存一致,避免深度附件/
        // 拷贝尺寸冲突;HUD 显示时缩小)
        int feedWidth = mc.getWindow().getWidth();
        int feedHeight = mc.getWindow().getHeight();
        if (feedTarget == null || feedTarget.width != feedWidth || feedTarget.height != feedHeight) {
            if (feedTarget != null) feedTarget.destroyBuffers();
            feedTarget = new TextureTarget("bza_pearl_feed", feedWidth, feedHeight, true);
        }

        // 相机参数:跟随珍珠时锚点 = 珍珠实时位置,朝向由珍珠速度接管;否则特写预测落点(朝向按配置)
        Vec3 anchor;
        double lift;
        float yaw;
        float pitch;
        ThrownEnderpearl pearl = EnderPearlTracker.trackedPearl();
        if (cfg.followPearl && pearl != null) {
            float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            anchor = pearl.getPosition(partialTick);
            // 与轨迹预测同一插值速度:直接读 deltaMovement 会在帧间跳变,相机朝向一顿一顿
            Vec3 motion = EnderPearlTracker.velocity(partialTick);
            if (motion == null || motion.lengthSqr() < 1.0E-6) {
                motion = pearl.getDeltaMovement();
            }
            if (motion.lengthSqr() < 1.0E-6) {
                motion = new Vec3(0.0, 0.0, 1.0); // 静止兜底:朝南看
            }
            // 由速度方向反推朝向(shootFromRotation 逆变换:yaw = atan2(-x, z),pitch = atan2(-y, 水平))
            yaw = (float) Math.toDegrees(Mth.atan2(-motion.x, motion.z));
            pitch = (float) Math.toDegrees(Mth.atan2(-motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z)));
            lift = 0;
        } else {
            // 落点特写:预测未命中(落点在虚空/超出模拟)时锚点没有实体方块,
            // 第二相机在虚空渲染会污染主画面(地形闪烁),直接不捕获
            if (!prediction.hit()) {
                feedReady = false;
                return;
            }
            anchor = prediction.landing();
            lift = MARKER_LIFT;
            var yawMode = cfg.pearlCameraYawMode == null ? CameraYawMode.FIXED : cfg.pearlCameraYawMode;
            yaw = switch (yawMode) {
                case FIXED -> 0.0F;
                case FRONT -> mc.player.getYRot();
                case BACK -> mc.player.getYRot() + 180.0F;
                case LEFT -> mc.player.getYRot() - 90.0F;
                case RIGHT -> mc.player.getYRot() + 90.0F;
            };
            // 偏航偏移 + 动态旋转只在固定角度生效
            if (yawMode == CameraYawMode.FIXED) {
                if (cfg.pearlCameraYawSpinSpeed > 0) {
                    // 用客户端毫秒(每帧平滑);gameTime 是服务器 tick(20Hz),旋转会一顿一顿
                    yaw += (float) ((Util.getMillis() % 100000L) / 1000.0 * cfg.pearlCameraYawSpinSpeed) % 360.0F;
                }
                yaw += cfg.pearlCameraYawOffset;
            }
            pitch = cfg.pearlCameraPitch;
        }
        feedReady = SecondCameraRenderer.capture(realDelta, new SecondCameraRenderer.CaptureParams(
                anchor, lift, yaw, pitch, cfg.pearlCameraDistance, cfg.pearlCameraViewDistance, feedTarget));
    }

    /// 画中画边框宽度(逻辑像素)
    private static final int FRAME_BORDER = 2;

    /// HUD 元素回调:把特写画面贴到 HUD 元素位置(支持编辑/缩放),左上角显示落地时间。
    public static void drawHud(GuiGraphicsExtractor graphics) {
        if (!feedReady || feedTarget == null) return;
        if (!HudManager.shouldShow("EnderPearlCamera")) return;
        var textureView = feedTarget.getColorTextureView();
        if (textureView == null) return;
        float s = HudManager.scale("EnderPearlCamera");
        var cfg = config();
        int dh = Math.max(1, Math.round(192 * s));
        // 显示比例按配置,从窗口比例纹理中裁切居中区域(渲染 target 是窗口尺寸,
        // mod 发光纹理按窗口尺寸缓存,比例设置通过 UV 裁切生效)
        var ratio = cfg.pearlCameraAspectRatio == null ? CameraAspectRatio.R2_1 : cfg.pearlCameraAspectRatio;
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
        int x0 = HudManager.x("EnderPearlCamera");
        int y0 = HudManager.y("EnderPearlCamera");
        GuiRenderState guiRenderState = graphics.guiRenderState;
        // 边框(背景色,像小地图):先画稍大的矩形,画中画覆盖在上面,四周露出边框
        var borderColor = cfg.pearlCameraBorderColor.getEffectiveColourRGB();
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
        // 落地时间(左上角;未命中预测不显示)
        if (cfg.showLandingTime && landingTimeSeconds >= 0) {
            graphics.text(Minecraft.getInstance().font,
                    "§a" + String.format("%.1fs", landingTimeSeconds),
                    x0 + 3, y0 + 3, 0xFFFFFFFF, true);
        }
    }

    private static PearlTrajectoryCfg config() {
        return ModConfigManager.get().kuudra.phase1.pearlTrajectory;
    }
}
