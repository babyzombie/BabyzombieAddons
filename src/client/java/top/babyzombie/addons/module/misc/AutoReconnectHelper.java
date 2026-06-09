package top.babyzombie.addons.module.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import top.babyzombie.addons.config.ModConfigManager;

public final class AutoReconnectHelper {

    private static String lastServerIp;
    private static String lastServerName;
    private static long lastJoinTimeNanos;
    private static int retryCount;
    private static int reconnectSecondsRemaining = -1;
    private static int tickCounter;
    private static boolean pendingReconnect;
    private static Screen pendingParentScreen;

    private AutoReconnectHelper() {}

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var server = client.getCurrentServer();
            if (server != null) {
                long now = System.nanoTime();
                if (lastJoinTimeNanos == 0 || (now - lastJoinTimeNanos) > 5_000_000_000L) {
                    lastServerIp = server.ip;
                    lastServerName = server.name;
                    retryCount = 0;
                }
                lastJoinTimeNanos = now;
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!pendingReconnect) return;
            pendingReconnect = false;
            var ip = lastServerIp;
            var name = lastServerName != null ? lastServerName : ip;
            if (ip == null || name == null) return;
            var freshServerData = new ServerData(name, ip, ServerData.Type.OTHER);
            ConnectScreen.startConnecting(
                    pendingParentScreen,
                    Minecraft.getInstance(),
                    ServerAddress.parseString(ip),
                    freshServerData,
                    false,
                    null
            );
        });
    }

    public static String getLastServerIp() {
        return lastServerIp;
    }

    public static boolean shouldStartCountdown() {
        var config = ModConfigManager.get();
        if (!config.general.autoReconnectEnabled) return false;
        if (lastServerIp == null) return false;
        if (retryCount >= config.general.autoReconnectMaxRetries && config.general.autoReconnectMaxRetries != 0) return false;
        return true;
    }

    public static int getDelay() {
        return ModConfigManager.get().general.autoReconnectDelay;
    }

    public static void startCountdown(int seconds) {
        reconnectSecondsRemaining = seconds;
        tickCounter = 0;
    }

    public static int tickCountdown() {
        if (reconnectSecondsRemaining <= 0) return -1;
        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;
            reconnectSecondsRemaining--;
        }
        return reconnectSecondsRemaining;
    }

    public static int getCountdownRemaining() {
        return reconnectSecondsRemaining;
    }

    public static int getRetryCount() {
        return retryCount;
    }

    public static void cancelCountdown() {
        reconnectSecondsRemaining = -1;
        tickCounter = 0;
    }

    public static void reconnect(Screen parentScreen) {
        var ip = lastServerIp;
        if (ip == null) return;
        retryCount++;
        cancelCountdown();
        pendingReconnect = true;
        pendingParentScreen = parentScreen;
    }
}
