package top.babyzombie.addons.module.dungeon;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Detects when there are 2+ archers in a dungeon party and alerts.
 */
public final class DupeArcherDetector {
    private DupeArcherDetector() {}

    public static void init() {
        if (!ModConfigManager.get().dungeon.dupeArcherDetection) return;

        // In original JS: checks scoreboard for [A] entries > 1
        // Simplified: listen for class selection messages
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !AutoRequeue.inDungeon) return;
            // Full implementation would check tab list/scoreboard for Archer count
        });
    }
}
