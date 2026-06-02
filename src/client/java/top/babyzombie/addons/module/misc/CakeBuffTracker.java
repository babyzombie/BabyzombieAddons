package top.babyzombie.addons.module.misc;

import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

public final class CakeBuffTracker {

    private static final Map<String, Integer> CAKES = new LinkedHashMap<>();
    static {
        CAKES.put("10❤ Health", 2); CAKES.put("3❈ Defense", 4);
        CAKES.put("2❁ Strength", 6); CAKES.put("10✦ Speed", 8);
        CAKES.put("5✎ Intelligence", 10); CAKES.put("2⫽ Ferocity", 12);
        CAKES.put("1♨ Vitality", 14); CAKES.put("1❂ True Defense", 16);
        CAKES.put("1α Sea Creature Chance", 18); CAKES.put("1✯ Magic Find", 20);
        CAKES.put("1♣ Pet Luck", 22); CAKES.put("1❄ Cold Resistance", 24);
        CAKES.put("10ф Rift Time", 26); CAKES.put("5☘ Mining Fortune", 28);
        CAKES.put("5☘ Farming Fortune", 30); CAKES.put("5☘ Foraging Fortune", 32);
    }

    private static final boolean[] found = new boolean[17]; // 1-indexed

    private CakeBuffTracker() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().general.cakeBuffTracker) return;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock() || !"Private Island".equals(tracker.getLocation())) return;

            String text = ChatUtils.stripColor(message.getString());
            if (!text.endsWith(" for 48 hours!")) return;
            String prefix;
            boolean refresh;
            if (text.startsWith("Yum! You gain +")) {
                prefix = "Yum! You gain +";
                refresh = false;
            } else if (text.startsWith("Big Yum! You refresh +")) {
                prefix = "Big Yum! You refresh +";
                refresh = true;
            } else return;

            String cakeName = text.substring(prefix.length(), text.length() - " for 48 hours!".length());
            Integer idx = CAKES.get(cakeName);
            if (idx == null) return;

            found[idx] = true;
            if (!refresh) ChatUtils.showMessage(buildChecklist());
        });
    }

    private static String buildChecklist() {
        var sb = new StringBuilder(512);
        sb.append("§f§m                                        §r");
        int eaten = 0;
        for (var entry : CAKES.entrySet()) {
            boolean has = found[entry.getValue()];
            sb.append("\n§c").append(entry.getKey()).append("   ");
            if (has) {
                sb.append("§a✔");
                eaten++;
            } else {
                sb.append("§c✘");
            }
        }
        sb.append("\n§f§m                                        §r");
        if (eaten == CAKES.size()) {
            return net.minecraft.network.chat.Component.translatable("babyzombieaddons.cake.all_eaten").getString();
        }
        return sb.toString();
    }
}
