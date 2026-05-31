package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;

/**
 * Tracks Pigman Sword ability cooldown via chat message detection.
 */
public final class PigmanSwordTimer {
    static long time;

    private PigmanSwordTimer() {}

    public static void init() {
        if (!ModConfigManager.get().slayer.pigmanSwordTimer) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = message.getString();
            if (text.contains("Pigman Sword")) {
                time = System.currentTimeMillis();
            }
        });
    }
}
