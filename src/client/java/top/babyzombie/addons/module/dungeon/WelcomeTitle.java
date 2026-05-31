package top.babyzombie.addons.module.dungeon;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;

/**
 * Shows a title screen when entering a dungeon floor.
 */
public final class WelcomeTitle {
    private WelcomeTitle() {}

    public static void init() {
        if (!ModConfigManager.get().dungeon.welcomeTitle) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (!text.contains("The Catacombs")) return;

            var player = Minecraft.getInstance().player;
            if (player == null) return;

            String title, subtitle = "";
            if (text.contains("Entrance")) {
                title = "§4§lThe Catacombs Entrance";
            } else if (text.contains("Master Mode")) {
                String floor = text.replaceAll(".*Floor (\\d+).*", "$1");
                title = "§4§lThe Catacombs Floor " + floor;
                subtitle = "§4§lMaster Mode";
            } else if (text.contains("Floor")) {
                String floor = text.replaceAll(".*Floor (\\d+).*", "$1");
                title = "§4§lThe Catacombs Floor " + floor;
            } else {
                title = "§4§lThe Catacombs";
            }

            player.displayClientMessage(net.minecraft.network.chat.Component.literal(title), true);
            final String sub = subtitle;
            if (!sub.isEmpty()) {
                Scheduler.schedule(2, () -> {
                    var p = Minecraft.getInstance().player;
                    if (p != null) p.displayClientMessage(net.minecraft.network.chat.Component.literal(sub), true);
                });
            }
        });
    }
}
