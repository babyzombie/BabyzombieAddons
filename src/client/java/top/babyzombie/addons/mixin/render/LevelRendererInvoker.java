package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface LevelRendererInvoker {

    @Invoker("extractEntity")
    EntityRenderState invokeExtractEntity(Entity entity, float partialTickTime);
}
