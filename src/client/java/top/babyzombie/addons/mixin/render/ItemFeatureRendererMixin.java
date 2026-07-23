package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.babyzombie.addons.util.render.DepthTestGlowRenderer;
import top.babyzombie.addons.util.render.DepthTestSubmitTracker;

@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {

    @ModifyVariable(method = "renderItem", at = @At("LOAD"), name = "outlineBufferSource")
    private OutlineBufferSource routeOutline(OutlineBufferSource outlineBufferSource,
            @Local(name = "submit") SubmitNodeStorage.ItemSubmit submit) {
        return DepthTestSubmitTracker.consume(submit)
            ? DepthTestGlowRenderer.getInstance().getBufferSource()
            : outlineBufferSource;
    }
}
