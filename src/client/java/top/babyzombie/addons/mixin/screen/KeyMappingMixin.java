package top.babyzombie.addons.mixin.screen;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.BabyzombieAddonsClient;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @Shadow public abstract boolean same(KeyMapping keyMapping);

    @Inject(method = "setDown", at = @At("HEAD"), cancellable = true)
    public void setDown$BabyzombieAddons(boolean bl, CallbackInfo ci) {
        if (!bl && BabyzombieAddonsClient.cancelKeyBindingRelease.isDown()
            && !this.same(BabyzombieAddonsClient.cancelKeyBindingRelease))
            ci.cancel();
    }
}
