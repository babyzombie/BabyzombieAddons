package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import top.babyzombie.addons.util.render.DepthTestMarker;

@Mixin(ItemFeatureRenderer.Submit.class)
public class RenderSubmitMixin implements DepthTestMarker {
    @Unique public boolean babyzombie$needsDepthTest;
    @Override public void babyzombie$setNeedsDepthTest(boolean v) { this.babyzombie$needsDepthTest = v; }
    @Override public boolean babyzombie$needsDepthTest() { return babyzombie$needsDepthTest; }
}
