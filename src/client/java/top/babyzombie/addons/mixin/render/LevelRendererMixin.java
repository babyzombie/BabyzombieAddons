package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.DepthTestGlowRenderer;
import top.babyzombie.addons.util.render.DepthTestSubmitTracker;

/**
 * - 深度拷贝：entity_outline 清空后复制主场景深度到自定义深度纹理
 * - 实体追踪：submitEntities 中通过 ThreadLocal 标记当前实体
 * - 自定义 flush：原版 outline 渲染后刷新深度测试发光缓冲区
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    // ── 深度拷贝（26.2: clearColorAndDepthTextures 签名变了，用 Vector4fc） ──
    @Inject(
        method = "lambda$addMainPass$0",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures"
                + "(Lcom/mojang/blaze3d/textures/GpuTexture;Lorg/joml/Vector4fc;Lcom/mojang/blaze3d/textures/GpuTexture;D)V",
            shift = At.Shift.AFTER
        )
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

    // ── 自定义发光缓冲区刷新（26.2: endOutlineBatch → executeOutline） ──
    @Inject(method = "lambda$addMainPass$0", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeOutline()V",
        shift = At.Shift.AFTER))
    private void flushDepthTestOutlines(CallbackInfo ci) {
        DepthTestGlowRenderer.getInstance().endBatch();
        DepthTestSubmitTracker.clear();
    }
}
