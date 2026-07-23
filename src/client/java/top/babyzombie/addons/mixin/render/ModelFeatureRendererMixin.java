package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.babyzombie.addons.util.render.DepthTestGlowRenderer;
import top.babyzombie.addons.util.render.DepthTestSubmitTracker;

/**
 * 深度测试实体的 outline 路由到自定义 DepthTestGlowRenderer 的 buffer source，
 * 使其使用独立深度纹理进行遮挡剔除。
 */
@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {

    @ModifyVariable(method = "renderModel(Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;"
            + "Lnet/minecraft/client/renderer/rendertype/RenderType;"
            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            + "Lnet/minecraft/client/renderer/OutlineBufferSource;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V",
            at = @At("LOAD"), name = "outlineBufferSource")
    private <S> OutlineBufferSource routeOutline(OutlineBufferSource outlineBufferSource,
            @Local(name = "submit") SubmitNodeStorage.ModelSubmit<S> submit) {
        return DepthTestSubmitTracker.consume(submit)
            ? DepthTestGlowRenderer.getInstance().getBufferSource()
            : outlineBufferSource;
    }
}
