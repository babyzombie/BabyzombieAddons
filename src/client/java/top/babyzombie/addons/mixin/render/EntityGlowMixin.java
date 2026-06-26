package top.babyzombie.addons.mixin.render;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.util.render.GlowController;

@Mixin(Entity.class)
public class EntityGlowMixin {

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void overrideTeamColor(CallbackInfoReturnable<Integer> cir) {
        if (GlowController.shouldGlow((Entity) (Object) this)) {
            cir.setReturnValue(GlowController.getGlowColor((Entity) (Object) this));
        }
    }
}
