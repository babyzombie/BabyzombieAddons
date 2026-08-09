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
        //
        // 必须等 client.isGameLoadFinished():MC 26.1 启动时资源包是异步加载的,
        // 首个 tick 时 screen 仍是 GenericMessageScreen + LoadingOverlay,
        // 此时发起连接会被加载完成后的 setScreen(TitleScreen) 顶掉,
        // 自动进服务器就不生效(此前靠"取消加载屏幕"类模组提前完成加载才正常)。
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (hasJoined) return;
            if (!client.isGameLoadFinished()) return;
            if (!ModConfigManager.get().general.autoJoinServer.enabled) return;
            String ip = ModConfigManager.get().general.autoJoinServer.ip;
            if (ip.isBlank()) return;
            hasJoined = true;

            ServerAddress address = ServerAddress.parseString(ip);
            ConnectScreen.startConnecting(
                    client.screen,
                    client,
                    address,
                    new ServerData(ip, ip, ServerData.Type.OTHER),
                    false,
                    null
            );
        });
    }
}
