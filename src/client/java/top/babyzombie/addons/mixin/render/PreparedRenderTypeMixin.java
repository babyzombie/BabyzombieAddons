package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import top.babyzombie.addons.util.render.DepthTestRenderPipelines;
import top.babyzombie.addons.util.render.GlowDepthRenderer;

/**
 * 深度测试 outline 渲染时，把深度附件替换成 GlowDepthRenderer 的独立深度纹理
 * （照搬 Skyblocker PreparedRenderTypeMixin 方案）。
 */
@Mixin(PreparedRenderType.class)
public abstract class PreparedRenderTypeMixin {
    @Shadow
    public abstract RenderPipeline pipeline();

    @ModifyExpressionValue(method = "drawFromBuffer(Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/IndexType;III)V",
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;getDepthTextureView()Lcom/mojang/blaze3d/textures/GpuTextureView;"))
    private GpuTextureView useGlowDepthTex(GpuTextureView original) {
        if (this.pipeline() == DepthTestRenderPipelines.OUTLINE_CULL
            || this.pipeline() == DepthTestRenderPipelines.OUTLINE_NO_CULL) {
            return GlowDepthRenderer.INSTANCE.getGlowDepthTexture();
        }
        return original;
    }
}
