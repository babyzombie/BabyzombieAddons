package top.babyzombie.addons.module.autoconnect;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import top.babyzombie.addons.config.ModConfigManager;

public final class AutoJoinModule {
    private static boolean hasJoined;

    private AutoJoinModule() {}

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (hasJoined || !(screen instanceof TitleScreen)) return;
            if (!ModConfigManager.get().autoJoin.autoJoinServer) return;
            String ip = ModConfigManager.get().autoJoin.autoJoinServerIP;
            if (ip.isBlank()) return;
            hasJoined = true;

            ServerAddress address = ServerAddress.parseString(ip);
            ConnectScreen.startConnecting(
                    screen,
                    client,
                    address,
                    new ServerData(ip, ip, ServerData.Type.OTHER),
                    false,
                    null
            );
        });
    }
}
