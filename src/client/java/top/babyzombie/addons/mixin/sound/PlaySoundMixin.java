package top.babyzombie.addons.mixin.sound;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.event.PlaySoundEvents;

@Mixin(SoundEngine.class)
public class PlaySoundMixin {

    /**
     * Modify the SoundInstance after early validation, right before getIdentifier() is consumed.
     * This ensures only sounds that will actually play are modified.
     */
    @ModifyVariable(method = "play", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getIdentifier()Lnet/minecraft/resources/Identifier;"),
            argsOnly = true, name = "instance")
    private SoundInstance modifySound(SoundInstance instance) {
        return PlaySoundEvents.MODIFY.invoker().modify(instance);
    }

    /** Before-play cancellation — fires AFTER MODIFY. */
    @Inject(method = "play", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getIdentifier()Lnet/minecraft/resources/Identifier;"),
            cancellable = true)
    private void beforePlay(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (PlaySoundEvents.BEFORE_PLAY.invoker().beforePlay(instance)) {
            cir.cancel();
        }
    }
}
