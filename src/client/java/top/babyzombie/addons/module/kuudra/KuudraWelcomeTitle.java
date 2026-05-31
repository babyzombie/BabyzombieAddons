package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Shows a title screen when entering a Kuudra instance.
 */
public final class KuudraWelcomeTitle {
    private KuudraWelcomeTitle() {}

    public static void init() {
        if (!ModConfigManager.get().kuudra.welcomeTitle) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (!text.contains("Kuudra")) return;

            String subtitle = "";
            if (text.contains("T1")) subtitle = "§e§lBasic Tier (T1)";
            else if (text.contains("T2")) subtitle = "§e§lHot Tier (T2)";
            else if (text.contains("T3")) subtitle = "§e§lBurning Tier (T3)";
            else if (text.contains("T4")) subtitle = "§e§lFiery Tier (T4)";
            else if (text.contains("T5")) subtitle = "§e§lInfernal Tier (T5)";
            else return;

            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§b§lKuudra"), true);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(subtitle), true);
            }
        });
    }
}
