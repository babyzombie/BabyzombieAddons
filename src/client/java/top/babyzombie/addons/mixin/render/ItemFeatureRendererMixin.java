package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.babyzombie.addons.util.render.DepthTestMarker;
import top.babyzombie.addons.util.render.GlowRenderTypeHolder;

import java.util.Optional;

@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {

    @ModifyExpressionValue(method = "prepareOutlineSubmit", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderType;outline()Ljava/util/Optional;"))
    private Optional<RenderType> useDepthRenderType(Optional<RenderType> original,
            ItemFeatureRenderer.Submit submit, @Local(name = "material") BakedQuad.MaterialInfo material) {
        if (((DepthTestMarker) (Object) submit).babyzombie$needsDepthTest()) {
            return material.itemRenderType() instanceof GlowRenderTypeHolder h
                ? h.babyzombie$getGlowRenderType() : original;
        }
        return original;
    }
}
