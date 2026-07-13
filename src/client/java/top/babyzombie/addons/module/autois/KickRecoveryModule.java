package top.babyzombie.addons.module.autois;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;

/**
 * Handles automatic /lobby (and optional /play skyblock) after being kicked
 * from a SkyBlock server. Operates independently of the autoIS toggle.
 */
public final class KickRecoveryModule {

    private static boolean lobbyConfirmRegistered;

    private KickRecoveryModule() {}

    public static void init() {
        // Detect kick / failed-warp messages
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            var cfg = ModConfigManager.get().skyblock;
            if (cfg.autois.autoBackToSkyblock == ModConfig.KickRecovery.OFF) return;
            if (message.getString().equals("You were kicked while joining that server!")
                    || message.getString().equals("你被踢出了该服务器！")
                    || message.getString().equals("Oops! You are not on SkyBlock so we couldn't warp you!")) {
                ChatUtils.sendCommand("lobby");
                lobbyConfirmRegistered = true;
                Scheduler.schedule(20, () -> lobbyConfirmRegistered = false);

                // /play skyblock only when autoIS is NOT active (autoIS handles it with higher priority)
                if (cfg.autois.autoBackToSkyblock == ModConfig.KickRecovery.LOBBY_AND_SKYBLOCK && !cfg.autois.enabled) {
                    Scheduler.schedule(1300, () -> ChatUtils.sendCommand("play skyblock"));
                }
            }
        });

        // Handle /lobby double-confirmation prompt
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !lobbyConfirmRegistered) return;
            if (message.getString().equals("Are you sure? Type /lobby again if you really want to quit.")) {
                ChatUtils.sendCommand("lobby");
                lobbyConfirmRegistered = false;
            }
        });
    }
}
