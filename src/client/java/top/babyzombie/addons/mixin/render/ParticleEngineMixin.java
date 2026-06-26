package top.babyzombie.addons.mixin.render;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.event.ParticleRenderEvents;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void onAdd(Particle p, CallbackInfo ci) {
        if (ParticleRenderEvents.BEFORE_ADD.invoker().beforeAdd(p)) {
            ci.cancel();
        }
    }
}
