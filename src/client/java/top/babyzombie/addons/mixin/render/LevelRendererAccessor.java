package top.babyzombie.addons.mixin.render;

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
}
