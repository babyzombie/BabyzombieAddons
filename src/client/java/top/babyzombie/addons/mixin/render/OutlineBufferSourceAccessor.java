package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OutlineBufferSource.class)
public interface OutlineBufferSourceAccessor {

    @Accessor
    @Mutable
    void setOutlineBufferSource(MultiBufferSource.BufferSource bufferSource);
}
