package top.babyzombie.addons.module.dungeon;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;

/**
 * Auto-instancerequeue on dungeon win or loss.
 */
public final class AutoRequeue {
    static boolean inDungeon;
    static int dailyRuns;

    private AutoRequeue() {}

    public static void init() {
        if (!ModConfigManager.get().dungeon.autoRequeue) return;

        // Track dungeon entry
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("The Catacombs")) inDungeon = true;
        });

        // Auto requeue on completion
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !inDungeon) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("Dungeon cleared") || text.contains("Defeat")) {
                dailyRuns++;
                Scheduler.schedule(100, () -> {
                    ChatUtils.sendCommand("instancerequeue");
                    inDungeon = false;
                });
            }
        });
    }
}
