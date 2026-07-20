package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.render.GlowRenderer;

/**
 * 在 entity_outline target 清空后、submitEntities 之前，
 * 将主场景深度拷贝到 entity_outline 的深度纹理中。
 * 配合 {@link top.babyzombie.addons.mixin.render.RenderPipelineMixin} 启用 OUTLINE 管线的深度测试，
 * 实现不透墙的实体发光。
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    @Final
    private @Nullable RenderTarget entityOutlineTarget;

    @Shadow
    @Final
    private LevelTargetBundle targets;

    /**
     * MC 26.2: addMainPass 变成了普通方法（不再是 lambda$addMainPass$0）。
     * 在 solid features 执行前注入（紧随 entity outline clear 之后）。
     */
    @Inject(
            method = "lambda$addMainPass$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;Lorg/joml/Vector4fc;Lcom/mojang/blaze3d/textures/GpuTexture;D)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onOutlineTargetCleared(CallbackInfo ci) {
        if (!GlowController.isAnyDepthTestRequested()) return;
        if (entityOutlineTarget == null) return;

        var mainRenderTarget = targets.main.get();
        var mainDepth = mainRenderTarget.getDepthTexture();
        var outlineDepth = entityOutlineTarget.getDepthTexture();
        if (mainDepth == null || outlineDepth == null) return;
        if (mainDepth.getWidth(0) != outlineDepth.getWidth(0)
                || mainDepth.getHeight(0) != outlineDepth.getHeight(0)) return;

        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                mainDepth, outlineDepth,
                0, 0, 0, 0, 0,
                mainDepth.getWidth(0), mainDepth.getHeight(0)
        );

        GlowRenderer.markDepthTestActive();
    }

    /** 原 OutlineBufferSourceMixin.endOutlineBatch 注入，MC 26.2 中改为 executeOutline 后。 */
    @Inject(
            method = "lambda$addMainPass$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeOutline()V",
                    shift = At.Shift.AFTER
            )
    )
    private void afterExecuteOutline(CallbackInfo ci) {
        GlowRenderer.endDepthTestedOutline();
    }
}
