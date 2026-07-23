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

/**
 * - 追踪 submitEntities 中当前实体的 EntityRenderState（通过 ThreadLocal）
 * - 在原版 endOutlineBatch 后拷贝深度 + 刷新深度测试发光缓冲区
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    // ── 深度拷贝（entity_outline 清空后，实体渲染前） ──
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
    @Inject(method = "lambda$addMainPass$0", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V",
        shift = At.Shift.AFTER))
    private void flushDepthTestOutlines(CallbackInfo ci) {
        DepthTestGlowRenderer.getInstance().endBatch();
        DepthTestSubmitTracker.clear();
    }
}
