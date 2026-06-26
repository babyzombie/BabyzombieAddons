package top.babyzombie.addons.mixin.chat;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.event.SendCommandEvents;

@Mixin(ClientPacketListener.class)
public class SendCommandMixin {

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void onSendCommand(String command, CallbackInfo ci) {
        if (SendCommandEvents.BEFORE_SEND.invoker().beforeSend(command.trim())) {
            ci.cancel();
        }
    }

    @Inject(method = "sendCommand", at = @At("TAIL"))
    private void afterSendCommand(String command, CallbackInfo ci) {
        SendCommandEvents.AFTER_SEND.invoker().afterSend(command.trim());
    }
}
