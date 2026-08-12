package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.DepthTestGlowRenderer;
import top.babyzombie.addons.util.render.DepthTestSubmitTracker;
import top.babyzombie.addons.util.render.SecondCameraRenderer;

/**
 * - 追踪 submitEntities 中当前实体的 EntityRenderState（通过 ThreadLocal）
 * - 在原版 endOutlineBatch 后拷贝深度 + 刷新深度测试发光缓冲区
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    // ── 深度拷贝(entity_outline 清空后,实体渲染前) ──
    // 拷贝时机保持在实体前:实体后拷贝会与实体渲染冲突导致实体表面闪烁。
    // depthTexture 只含地形,描边(NO_CULL)背面像素会漏一点(可接受)。
    @Inject(
        method = "lambda$addMainPass$0",
        slice = @Slice(from = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;shouldShowEntityOutlines()Z")),
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures"
                + "(Lcom/mojang/blaze3d/textures/GpuTexture;ILcom/mojang/blaze3d/textures/GpuTexture;D)V",
            ordinal = 0, shift = At.Shift.AFTER)
    )
    private void copyDepthForGlow(CallbackInfo ci) {
        DepthTestGlowRenderer.getInstance().updateDepth();
    }

    // ── 追踪当前实体 → ThreadLocal ──
    @Inject(method = "submitEntities", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;submit"
            + "(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;"
            + "DDDLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;)V")
    )
    private void markCurrentEntity(CallbackInfo ci,
            @Local(name = "state") EntityRenderState state) {
        DepthTestSubmitTracker.CURRENT_ENTITY_STATE.set(state);
    }

    @Inject(method = "submitEntities", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;submit"
            + "(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;"
            + "DDDLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
        shift = At.Shift.AFTER)
    )
    private void clearCurrentEntity(CallbackInfo ci) {
        DepthTestSubmitTracker.CURRENT_ENTITY_STATE.remove();
    }

    // ── 自定义发光缓冲区刷新 ──
    // 第二相机捕获期间:endOutlineBatch 前把 outline 输出导向子相机 outline target
    // (RenderType 的 outputTarget 是构造时缓存的引用,替换字段无效,override 优先);
    // 原版 outline 画完后(本方法)刷新深度测试发光(其内部用 outputDepthTextureOverride
    // 指向独立深度纹理),全部画完再恢复 override。深度不 override:挡掉会失效。
    @Inject(method = "lambda$addMainPass$0", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"))
    private void beforeOutline(CallbackInfo ci) {
        if (SecondCameraRenderer.capturing) SecondCameraRenderer.beginOutlineOverride();
    }

    @Inject(method = "lambda$addMainPass$0", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V",
        shift = At.Shift.AFTER))
    private void flushDepthTestOutlines(CallbackInfo ci) {
        DepthTestGlowRenderer.getInstance().endBatch();
        DepthTestSubmitTracker.clear();
        if (SecondCameraRenderer.capturing) SecondCameraRenderer.endOutlineOverride();
    }
}
