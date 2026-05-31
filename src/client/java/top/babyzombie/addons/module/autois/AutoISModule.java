package top.babyzombie.addons.module.autois;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.Scheduler;

/**
 * Auto-return to Private Island or Garden after world load.
 */
public final class AutoISModule {
    private AutoISModule() {}

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!ModConfigManager.get().general.autois) return;
            Scheduler.schedule(60, () -> {
                var tracker = HypixelLocationTracker.getInstance();
                if (!tracker.isInSkyblock()) return;
                String lobby = tracker.getLobbyName();
                if (lobby == null) return;
                if (lobby.contains("Private Island") || lobby.contains("Garden")) return;
                ChatUtils.sendCommand("warp garden");
            });
        });
    }
}
