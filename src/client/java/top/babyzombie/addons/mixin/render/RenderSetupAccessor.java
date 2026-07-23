package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(RenderSetup.class)
public interface RenderSetupAccessor {
    @Accessor("textures")
    Map<String, Object> getTextureBindings(); // TextureBinding is package-private

    @Accessor
    RenderPipeline getPipeline();

    @Accessor
    RenderSetup.OutlineProperty getOutlineProperty();
}
