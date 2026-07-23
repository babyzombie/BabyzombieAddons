package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.util.render.GlowRenderer;

@Mixin(RenderPipeline.class)
public class RenderPipelineMixin {
    @Inject(method = "getDepthStencilState", at = @At("HEAD"), cancellable = true)
    private void overrideDepthStencilForOutline(CallbackInfoReturnable<DepthStencilState> cir) {
        RenderPipeline self = (RenderPipeline) (Object) this;
        if (self != RenderPipelines.OUTLINE_CULL && self != RenderPipelines.OUTLINE_NO_CULL) return;
        if (GlowRenderer.isDepthTestActive()) {
            cir.setReturnValue(DepthStencilState.DEFAULT);
        }
    }
}
