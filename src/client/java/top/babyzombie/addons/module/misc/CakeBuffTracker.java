package top.babyzombie.addons.module.misc;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Replaces default cake messages with a checklist of consumed cakes.
 */
public final class CakeBuffTracker {
    static int cakesFound;

    private CakeBuffTracker() {}

    public static void init() {
        if (!ModConfigManager.get().general.cakeBuffTracker) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("cake") && (text.contains("eaten") || text.contains("buff"))) {
                cakesFound++;
            }
        });
    }
}
