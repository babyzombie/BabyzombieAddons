package top.babyzombie.addons.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.babyzombie.addons.config.ModConfigManager;

/**
 * Lava fishing bobber fix: the vanilla client simulation only treats WATER
 * as a liquid surface, so on Hypixel lava fishing the bobber keeps sinking
 * locally until the server force-syncs it back every 60 ticks. Treat lava
 * as a liquid surface too, so the bobber floats on lava and only sinks
 * when biting.
 */
@Mixin(FishingHook.class)
public class LavaBobberFixMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean bza$treatLavaAsLiquidSurface(FluidState state, TagKey<Fluid> tag, Operation<Boolean> original) {
        if (ModConfigManager.get().fishing.lavaBobberFix && tag == FluidTags.WATER && state.is(FluidTags.LAVA)) {
            return true;
        }
        return original.call(state, tag);
    }
}
