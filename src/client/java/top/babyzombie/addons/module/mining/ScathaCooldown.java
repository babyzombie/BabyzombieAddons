package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Tracks 30-second Scatha/Worm spawn cooldown.
 */
public final class ScathaCooldown {
    static long time;

    private ScathaCooldown() {}

    public static void init() {
        if (!ModConfigManager.get().mining.scathaCooldown) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if ((text.contains("Scatha") || text.contains("Worm")) && text.contains("spawn")) {
                time = System.currentTimeMillis();
            }
        });
    }
}
