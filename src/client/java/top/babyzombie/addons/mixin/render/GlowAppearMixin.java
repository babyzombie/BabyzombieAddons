package top.babyzombie.addons.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.util.render.GlowController;

@Mixin(Minecraft.class)
public class GlowAppearMixin {

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void forceGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (GlowController.shouldGlow(entity)) {
            cir.setReturnValue(true);
        }
    }
}
