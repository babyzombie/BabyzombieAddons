package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelExtractor.class)
public interface LevelExtractorInvoker {

    @Invoker("extractEntity")
    EntityRenderState invokeExtractEntity(Entity entity, float partialTickTime);
}
