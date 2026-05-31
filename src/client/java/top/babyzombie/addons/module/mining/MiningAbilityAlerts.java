package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Shows title alerts when mining abilities are ready, used, or expired.
 */
public final class MiningAbilityAlerts {
    static long readyTime;

    private MiningAbilityAlerts() {}

    public static void init() {
        if (!ModConfigManager.get().mining.miningAbilityAlerts) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("ability is ready") || text.contains("now available")) {
                readyTime = System.currentTimeMillis();
                var player = Minecraft.getInstance().player;
                if (player != null)
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§a§lABILITY READY!"), true);
            }
        });
    }
}
