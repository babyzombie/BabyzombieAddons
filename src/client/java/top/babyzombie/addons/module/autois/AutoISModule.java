package top.babyzombie.addons.module.autois;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.ServerTick;

public final class AutoISModule {

    private static boolean lobbyConfirmRegistered;
    private static long kickTime;

    private AutoISModule() {}

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!ModConfigManager.get().general.autois) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            int delayTicks = ModConfigManager.get().general.autoisDelay * 20;
            Scheduler.schedule(delayTicks, AutoISModule::doWarp);
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !ModConfigManager.get().general.autois) return;
            if (message.getString().contains("You were kicked")
                    || message.getString().contains("Oops! You are not on SkyBlock")) {
                kickTime = ServerTick.getTime();
                var mode = ModConfigManager.get().general.autoBackToSkyblock;
                if (mode != ModConfig.KickRecovery.OFF) {
                    lobbyConfirmRegistered = true;
                    ChatUtils.sendCommand("lobby");
                }
                if (mode == ModConfig.KickRecovery.LOBBY_AND_SKYBLOCK) {
                    Scheduler.schedule(1200, () -> {
                        if (ServerTick.getTime() - kickTime < 1300 * 50)
                            ChatUtils.sendCommand("play skyblock");
                    });
                }
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !lobbyConfirmRegistered) return;
            if (message.getString().contains("Are you sure?")
                    || message.getString().contains("Type /lobby again")) {
                ChatUtils.sendCommand("lobby");
                lobbyConfirmRegistered = false;
            }
        });

        Scheduler.schedule(20, () -> lobbyConfirmRegistered = false);
    }

    private static void doWarp() {
        if (!ModConfigManager.get().general.autois) return;
        var tracker = HypixelLocationTracker.getInstance();
        if (tracker.isInSkyblock()) {
            String island = tracker.getMap();
            if (island == null) return;
            var dest = ModConfigManager.get().general.autoisDest;
            if (dest == ModConfig.AutoISDest.ISLAND && !"Private Island".equals(island))
                ChatUtils.sendCommand("is");
            else if (dest == ModConfig.AutoISDest.GARDEN && !"Garden".equals(island))
                ChatUtils.sendCommand("warp garden");
        } else {
            var recovery = ModConfigManager.get().general.autoBackToSkyblock;
            if (recovery != ModConfig.KickRecovery.OFF) {
                ChatUtils.sendCommand("lobby");
                if (recovery == ModConfig.KickRecovery.LOBBY_AND_SKYBLOCK)
                    Scheduler.schedule(100, () -> ChatUtils.sendCommand("play skyblock"));
            }
        }
    }
}
