package top.babyzombie.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.event.WorldChangeCallback;
import top.babyzombie.addons.util.ServerTick;

@Mixin(Minecraft.class)
public class ClientWorldChangeMixin {

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void onSetLevel(ClientLevel clientLevel, CallbackInfo ci) {
        WorldChangeCallback.fire((Minecraft) (Object) this, clientLevel);
    }
}
