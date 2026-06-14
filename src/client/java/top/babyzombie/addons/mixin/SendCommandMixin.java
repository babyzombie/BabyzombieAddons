package top.babyzombie.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.abiphone.AbiphoneContactScreen;
import top.babyzombie.addons.module.abiphone.AbiphoneTracker;
import top.babyzombie.addons.module.playcmd.PlayCmdModule;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

@Mixin(ClientPacketListener.class)
public class SendCommandMixin {

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void onSendCommand(String string, CallbackInfo ci) {
        if (!HypixelLocationTracker.getInstance().isOnHypixel()) return;
        String cmd = string.trim();

        // /call → Abiphone contact screen
        if (cmd.equals("call") && HypixelLocationTracker.getInstance().isInSkyblock() && ModConfigManager.get().misc.abiphoneGui) {
            ci.cancel();
            var client = Minecraft.getInstance();
            var tracker = HypixelLocationTracker.getInstance();
            var contacts = AbiphoneTracker.getInstance()
                .loadItems(tracker.getUuid(), tracker.getProfileId());
            client.execute(() -> client.setScreenAndShow(new AbiphoneContactScreen(contacts)));
            return;
        }

        // /play → Play command GUI
        if (cmd.equals("play") && ModConfigManager.get().misc.playCmd) {
            ci.cancel();
            PlayCmdModule.openGUI();
        }
    }
}
