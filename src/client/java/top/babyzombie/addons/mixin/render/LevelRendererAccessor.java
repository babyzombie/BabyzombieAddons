package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor("viewArea")
    ViewArea getViewArea();

    @Accessor("entityRenderDispatcher")
    EntityRenderDispatcher getEntityRenderDispatcher();

    @Accessor("chunkLayerSampler")
    GpuSampler getChunkLayerSampler();

    @Accessor("chunkLayerSampler")
    void setChunkLayerSampler(GpuSampler chunkLayerSampler);
}
