package top.babyzombie.addons.module.hunting.safari;

import com.mojang.blaze3d.GpuFormat;
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
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import top.babyzombie.addons.config.FishingConfig.CameraAspectRatio;
import top.babyzombie.addons.config.FishingConfig.CameraYawMode;
import top.babyzombie.addons.config.HuntingConfig.SafariCapsuleCameraConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.event.AfterWorldRenderEvents;
import top.babyzombie.addons.util.render.SecondCameraRenderer;

/// Safari 精灵球落地镜头:手持胶囊瞄准时,用第二相机在预测落点显示特写画面。
/// 复用 SecondCameraRenderer(与钓鱼相机同一套渲染机制);
/// 落点实时由 SafariTrajectory.predictedLanding() 提供(与轨迹线一致)。
public final class SafariCapsuleCamera {

    /// 特写画面高度(物理像素),宽度按配置的画面比例
    private static final int FEED_HEIGHT = 256;
    /// 落点在方块表面,marker 微抬升脱离碰撞即可
    private static final double MARKER_LIFT = 0.3;
    /// 帧率限制(渲染时间戳)
    private static long lastRenderMillis;

    private static @Nullable TextureTarget feedTarget;
    /// 本帧是否成功捕获,供 HUD 判断是否绘制
    private static boolean feedReady;

    private SafariCapsuleCamera() {}

    public static void init() {
        // 捕获时机与钓鱼相机相同(renderLevel 完全返回后)
        AfterWorldRenderEvents.register(SafariCapsuleCamera::capture);
        // HUD 元素:渲染在 HUD 层,设置界面等 screen 打开时自动被遮挡
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "safari_capsule_camera"),
                (graphics, tickCounter) -> {
            if (!feedReady || feedTarget == null) return;
            if (!HudManager.shouldShow("SafariCapsuleCamera")) return;
            drawHud(graphics);
        });
    }

    private static void capture(DeltaTracker realDelta) {
        var mc = Minecraft.getInstance();
        var cfg = ModConfigManager.get().hunting.safari.trajectory.capsuleCamera;
        if (!cfg.enabled || mc.level == null || mc.player == null) {
            feedReady = false;
            return;
        }
        // 手持胶囊(瞄准)时才显示;放下/换武器立即关闭
        if (!SafariTrajectory.isCapsule(mc.player.getMainHandItem())
                && !SafariTrajectory.isCapsule(mc.player.getOffhandItem())) {
            feedReady = false;
            return;
        }
        // 帧率限制(按秒):距离上次渲染不足 1/fps 秒则跳过,保留上一帧画面(feedReady 不清)
        long now = Util.getMillis();
        if (now - lastRenderMillis < 1000L / Math.max(1, cfg.frameRate)) return;
        lastRenderMillis = now;
        // 实时预测落点(与轨迹线一致;不在 Safari/预测失败时关闭)
        Vec3 landing = SafariTrajectory.predictedLanding();
        if (landing == null) {
            feedReady = false;
            return;
        }
        // 懒创建输出目标:窗口尺寸(与 mod 发光纹理按窗口尺寸缓存一致,避免深度附件/
        // 拷贝尺寸冲突;HUD 显示时缩小)
        int feedWidth = mc.getWindow().getWidth();
        int feedHeight = mc.getWindow().getHeight();
        if (feedTarget == null || feedTarget.width != feedWidth || feedTarget.height != feedHeight) {
            if (feedTarget != null) feedTarget.destroyBuffers();
            feedTarget = new TextureTarget("bza_safari_capsule_feed", feedWidth, feedHeight, true, GpuFormat.RGBA8_UNORM);
        }
        // 相机朝向按配置(yawMode),俯视配置的 pitch
        var yawMode = cfg.yawMode == null ? CameraYawMode.FIXED : cfg.yawMode;
        float yaw = switch (yawMode) {
            case FIXED -> 0.0F;
            case FRONT -> mc.player.getYRot();
            case BACK -> mc.player.getYRot() + 180.0F;
            case LEFT -> mc.player.getYRot() - 90.0F;
            case RIGHT -> mc.player.getYRot() + 90.0F;
        };
        // 偏航偏移 + 动态旋转只在固定角度生效
        if (yawMode == CameraYawMode.FIXED) {
            if (cfg.yawSpinSpeed > 0) {
                // 用客户端毫秒(每帧平滑);gameTime 是服务器 tick(20Hz),旋转会一顿一顿
                yaw += (float) ((Util.getMillis() % 100000L) / 1000.0 * cfg.yawSpinSpeed) % 360.0F;
            }
            yaw += cfg.yawOffset;
        }
        feedReady = SecondCameraRenderer.capture(realDelta, new SecondCameraRenderer.CaptureParams(
                landing, MARKER_LIFT, yaw, cfg.pitch, cfg.distance, cfg.viewDistance, feedTarget));
    }

    /// 画中画边框宽度(逻辑像素)
    private static final int FRAME_BORDER = 2;

    /// HUD 元素回调:把特写画面贴到 HUD 元素位置(支持编辑/缩放)。
    public static void drawHud(GuiGraphicsExtractor graphics) {
        if (!feedReady || feedTarget == null) return;
        if (!HudManager.shouldShow("SafariCapsuleCamera")) return;
        var textureView = feedTarget.getColorTextureView();
        if (textureView == null) return;
        float s = HudManager.scale("SafariCapsuleCamera");
        int dh = Math.max(1, Math.round(192 * s));
        int dw = Math.max(1, Math.round(dh * feedTarget.width / (float) Math.max(1, feedTarget.height)));
        int x0 = HudManager.x("SafariCapsuleCamera");
        int y0 = HudManager.y("SafariCapsuleCamera");
        GuiRenderState guiRenderState = graphics.guiRenderState;
        // 边框(背景色,像小地图):先画稍大的矩形,画中画覆盖在上面,四周露出边框
        var borderColor = ModConfigManager.get().hunting.safari.trajectory.capsuleCamera.borderColor.getEffectiveColourRGB();
        guiRenderState.addGuiElement(new ColoredRectangleRenderState(
                RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(),
                x0 - FRAME_BORDER, y0 - FRAME_BORDER, x0 + dw + FRAME_BORDER, y0 + dh + FRAME_BORDER,
                borderColor, borderColor, null));
        guiRenderState.addBlitToCurrentLayer(new BlitRenderState(
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                TextureSetup.singleTexture(textureView,
                        RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                new Matrix3x2f(), x0, y0, x0 + dw, y0 + dh,
                0.0F, 1.0F, 1.0F, 0.0F, -1, null, null));
    }
}
