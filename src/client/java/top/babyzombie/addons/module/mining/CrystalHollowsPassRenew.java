package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

public final class CrystalHollowsPassRenew {
    private CrystalHollowsPassRenew() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.crystalHollows.passAutoRenew) return;
            if (ChatUtils.stripColor(message.getString())
                    .equals("Click here to purchase a new 6 hour pass for 10,000 Coins")) {
                ChatUtils.sendCommand("purchasecrystalhollowspass");
            }
        });
    }
}
