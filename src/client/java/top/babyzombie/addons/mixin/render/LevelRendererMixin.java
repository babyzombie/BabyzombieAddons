package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.CurrentEntityTracker;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.render.GlowDepthRenderer;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow @Final private @Nullable RenderTarget entityOutlineTarget;
    @Shadow @Final private LevelTargetBundle targets;

    // ── 深度拷贝 ──
    // 时机与 Skyblocker 一致：第一个 clearColorAndDepthTextures 之后（main 深度为上一帧完整深度）。
    // 26.2 帧图延迟执行下，executeSolid 后 main 深度可能尚未写入，拷贝会拿到无效内容。
    @Inject(method = "lambda$addMainPass$0",
        slice = @Slice(from = @At(value = "FIELD",
            target = "Lnet/minecraft/client/renderer/state/level/LevelRenderState;shouldShowEntityOutlines:Z",
            opcode = Opcodes.GETFIELD)),
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures"
                + "(Lcom/mojang/blaze3d/textures/GpuTexture;Lorg/joml/Vector4fc;"
                + "Lcom/mojang/blaze3d/textures/GpuTexture;D)V",
            ordinal = 0, shift = At.Shift.AFTER))
    private void copyDepth(CallbackInfo ci) {
        // 照搬 Skyblocker 方案：主场景深度拷到独立深度纹理（PreparedRenderTypeMixin 用它替换 outline 深度附件）
        if (!GlowController.isAnyDepthTestRequested()) return;
        GlowDepthRenderer.INSTANCE.updateGlowDepthTexDepth();
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
