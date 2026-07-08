package top.babyzombie.addons.mixin.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.misc.AutoReconnectHelper;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {

    @Inject(method = "startConnecting", at = @At("HEAD"))
    private static void onStartConnecting(Screen parent, Minecraft minecraft, ServerAddress hostAndPort,
                                          ServerData data, boolean isQuickPlay,
                                          @Nullable TransferState transferState, CallbackInfo ci) {
        String ip;
        String host = hostAndPort.getHost();
        int port = hostAndPort.getPort();
        if (port == 25565) {
            ip = host;
        } else {
            ip = host + ":" + port;
        }
        AutoReconnectHelper.onConnectionAttempt(ip, data.name);

        // Auto-accept Hypixel Skyblock resource pack without prompting
        if (data != null
                && ModConfigManager.get().general.serverResourcePackAutoAccept
                && HypixelLocationTracker.getInstance().isOnHypixel()) {
            data.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        }
    }
}
