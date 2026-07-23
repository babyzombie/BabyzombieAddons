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

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow @Final private @Nullable RenderTarget entityOutlineTarget;
    @Shadow @Final private LevelTargetBundle targets;

    @Inject(
        method = "lambda$addMainPass$0",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures"
                + "(Lcom/mojang/blaze3d/textures/GpuTexture;Lorg/joml/Vector4fc;"
                + "Lcom/mojang/blaze3d/textures/GpuTexture;D)V",
            shift = At.Shift.AFTER))
    private void copyDepthForGlow(CallbackInfo ci) {
        if (!GlowController.isAnyDepthTestRequested()) return;
        if (entityOutlineTarget == null) return;
        var main = targets.main.get();
        var mainDepth = main.getDepthTexture();
        var outlineDepth = entityOutlineTarget.getDepthTexture();
        if (mainDepth == null || outlineDepth == null) return;
        if (mainDepth.getWidth(0) != outlineDepth.getWidth(0)
                || mainDepth.getHeight(0) != outlineDepth.getHeight(0)) return;
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                mainDepth, outlineDepth, 0, 0, 0, 0, 0,
                mainDepth.getWidth(0), mainDepth.getHeight(0));
        GlowRenderer.markDepthTestActive();
    }
}
