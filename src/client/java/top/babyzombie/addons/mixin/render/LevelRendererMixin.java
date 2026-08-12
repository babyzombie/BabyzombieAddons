package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.CurrentEntityTracker;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.render.GlowDepthRenderer;
import top.babyzombie.addons.util.render.SecondCameraRenderer;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
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

    // ── 第二相机 outline 输出导向 ──
    // PreparedRenderType 的输出是它自己的 outputTarget(构造时缓存的引用,替换字段无效),
    // drawFromBuffer 里 RenderSystem.outputColorTextureOverride 优先于 outputTarget;
    // 第二遍渲染时把 outline 节点导向子相机 outline target(不污染主画面,且子相机画面能合成发光)
    @Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeOutline()V"))
    private void beforeOutline(CallbackInfo ci) {
        if (SecondCameraRenderer.capturing) SecondCameraRenderer.beginOutlineOverride();
    }

    @Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeOutline()V",
            shift = At.Shift.AFTER))
    private void afterOutline(CallbackInfo ci) {
        if (SecondCameraRenderer.capturing) SecondCameraRenderer.endOutlineOverride();
    }

}
