package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Auto-refills ender pearls from sacks on Kuudra instance start.
 */
public final class EnderPearlRefill {
    private EnderPearlRefill() {}

    public static void init() {
        if (!ModConfigManager.get().kuudra.enderPearlRefill) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("Kuudra") && text.contains("instance")) {
                ChatUtils.sendCommand("gfs ender_pearl 16");
            }
        });
    }
}
