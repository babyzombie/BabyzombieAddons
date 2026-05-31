package top.babyzombie.addons.module.fishing;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

/**
 * Detects legendary sea creature catches and alerts with category info.
 */
public final class LegendaryAlerts {

    private static final String[] WATER = {"Sea Emperor", "Yeti", "Great White Shark", "Water Hydra"};
    private static final String[] LAVA = {"Lord Jawbus", "Thunder", "Magma Slug", "Fire Eel"};
    private static final String[] HOLIDAY = {"Grinch", "Nutcracker", "Reindrake", "Frozen Steve"};

    private LegendaryAlerts() {}

    public static void init() {
        if (!ModConfigManager.get().fishing.legendaryAlerts) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !HypixelLocationTracker.getInstance().isInSkyblock()) return;
            String text = ChatUtils.stripColor(message.getString());
            if (!text.contains("You caught a")) return;

            String creature = text.replace("You caught a ", "").replace("!", "").trim();
            String category = findCategory(creature);
            if (category == null) return;

            var player = Minecraft.getInstance().player;
            if (player != null) {
                String alert = String.format("§6§lLEGENDARY! §e%s §7[%s§7]", creature, category);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(alert), true);
                ChatUtils.sendCommand("pc " + alert);
            }
        });
    }

    private static String findCategory(String name) {
        for (String n : WATER) if (name.contains(n)) return "§bWATER";
        for (String n : LAVA) if (name.contains(n)) return "§cLAVA";
        for (String n : HOLIDAY) if (name.contains(n)) return "§aHOLIDAY";
        return null;
    }
}
