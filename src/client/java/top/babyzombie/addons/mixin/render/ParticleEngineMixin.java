package top.babyzombie.addons.mixin.render;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.event.ParticleRenderEvents;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    public void createParticle(ParticleOptions options, double x, double y, double z, double xa, double ya, double za, CallbackInfoReturnable<Particle> cir) {
        if (ParticleRenderEvents.BEFORE_CREATE.invoker().beforeCreate(options, x, y, z, xa, ya, za)) {
            cir.cancel();
        }
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void onAdd(Particle p, CallbackInfo cir) {
        if (ParticleRenderEvents.BEFORE_ADD.invoker().beforeAdd(p)) {
            cir.cancel();
        }
    }
}
