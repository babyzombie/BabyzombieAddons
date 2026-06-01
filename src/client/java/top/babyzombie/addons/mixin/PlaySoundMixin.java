package top.babyzombie.addons.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.event.PlaySoundEvents;

@Mixin(SoundEngine.class)
public class PlaySoundMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void beforePlay(SoundInstance soundInstance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (PlaySoundEvents.BEFORE_PLAY.invoker().beforePlay(soundInstance)) {
            cir.cancel();
        }
    }
}
