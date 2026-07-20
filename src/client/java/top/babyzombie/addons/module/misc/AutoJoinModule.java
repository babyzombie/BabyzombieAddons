package top.babyzombie.addons.module.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import top.babyzombie.addons.config.ModConfigManager;

public final class AutoJoinModule {
    private static boolean hasJoined;

    private AutoJoinModule() {}

    public static void init() {
        // START_CLIENT_TICK fires from the first frame (including title screen),
        // well after Minecraft.<init> has completed, so framerateLimitTracker is
        // guaranteed to be initialized.
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (hasJoined) return;
            if (!ModConfigManager.get().general.autoJoinServer.enabled) return;
            String ip = ModConfigManager.get().general.autoJoinServer.ip;
            if (ip.isBlank()) return;
            hasJoined = true;

            ServerAddress address = ServerAddress.parseString(ip);
            ConnectScreen.startConnecting(
                    client.gui.screen(),
                    client,
                    address,
                    new ServerData(ip, ip, ServerData.Type.OTHER),
                    false,
                    null
            );
        });
    }
}
