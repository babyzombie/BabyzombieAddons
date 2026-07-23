package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import top.babyzombie.addons.util.render.CurrentEntityTracker;
import top.babyzombie.addons.util.render.GlowController;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow @Final private @Nullable RenderTarget entityOutlineTarget;
    @Shadow @Final private LevelTargetBundle targets;

    // ── 深度拷贝 ──
    @Inject(method = "lambda$addMainPass$0", at = @At(
        value = "INVOKE",
        target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures"
            + "(Lcom/mojang/blaze3d/textures/GpuTexture;Lorg/joml/Vector4fc;"
            + "Lcom/mojang/blaze3d/textures/GpuTexture;D)V", shift = At.Shift.AFTER))
    private void copyDepth(CallbackInfo ci) {
        // 全局方案：复制主场景深度到 entity_outline
        if (!GlowController.isAnyDepthTestRequested()) return;
        if (entityOutlineTarget == null) return;
        var main = targets.main.get();
        var md = main.getDepthTexture();
        var od = entityOutlineTarget.getDepthTexture();
        if (md == null || od == null) return;
        if (md.getWidth(0) != od.getWidth(0) || md.getHeight(0) != od.getHeight(0)) return;
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(md, od, 0,0,0,0,0, md.getWidth(0), md.getHeight(0));
    }

    // ── 实体追踪 ──
    @Inject(method = "submitEntities", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;submit"
            + "(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;"
            + "DDDLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"))
    private void markEntity(CallbackInfo ci, @Local(name = "state") EntityRenderState state) {
        CurrentEntityTracker.STATE.set(state);
    }

    @Inject(method = "submitEntities", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;submit"
            + "(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;"
            + "DDDLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;)V", shift = At.Shift.AFTER))
    private void clearEntity(CallbackInfo ci) { CurrentEntityTracker.STATE.remove(); }
}
