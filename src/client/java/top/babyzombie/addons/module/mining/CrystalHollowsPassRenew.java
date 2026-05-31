package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Auto-purchases the 6-hour Crystal Hollows Pass when it expires.
 */
public final class CrystalHollowsPassRenew {
    private CrystalHollowsPassRenew() {}

    public static void init() {
        if (!ModConfigManager.get().mining.crystalHollowsPassAutoRenew) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("Your Pass is about to expire")) {
                ChatUtils.sendCommand("purchasecrystalhollowspass");
            }
        });
    }
}
