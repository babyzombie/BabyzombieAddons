package top.babyzombie.addons.mixin.sound;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.module.garden.XpOrbSoundReducer;

@Mixin(SoundEngine.class)
public class PlaySoundMixin {

    @Inject(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getIdentifier()Lnet/minecraft/resources/Identifier;"), cancellable = true)
    private void beforePlay(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (PlaySoundEvents.BEFORE_PLAY.invoker().beforePlay(instance)) {
            cir.cancel();
        }
    }

    @Redirect(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getVolume()F"))
    private float adjustVolume(SoundInstance instance) {
        float originalVolume = instance.getVolume();
        return XpOrbSoundReducer.getAdjustedVolume(instance, originalVolume);
    }
}
