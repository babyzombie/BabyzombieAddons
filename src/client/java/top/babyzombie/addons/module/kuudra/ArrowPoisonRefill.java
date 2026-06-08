package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.ServerTick;

import java.util.regex.Pattern;

/**
 * Refill arrow poisons when triggered in Kuudra.
 */
public final class ArrowPoisonRefill {

    private static final Pattern EATEN_BY_KUUDRA = Pattern.compile(
            "([0-9a-zA-Z_]{2,24}) has been eaten by Kuudra!");

    private static long cooldown;

    private ArrowPoisonRefill() {}

    public static void init() {
        // Toxic Arrow Poison — any Kuudra tier, when someone is eaten
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            if (overlay) return;
            int threshold = ModConfigManager.get().kuudra.toxicArrowThreshold;
            if (threshold <= 0) return;
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;

            String text = ChatUtils.stripColor(msg.getString());
            if (!EATEN_BY_KUUDRA.matcher(text).find()) return;

            long now = ServerTick.getTime();
            if (cooldown > now) return;
            cooldown = now + 2000;

            int current = countArrow("TOXIC_ARROW_POISON");
            if (current >= threshold) return;

            ChatUtils.sendCommand("gfs Toxic Arrow Poison " + (threshold - current));
        });

        // Twilight Arrow Poison — T5 only, when Elle says the success line
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            if (overlay) return;
            int threshold = ModConfigManager.get().kuudra.twilightArrowThreshold;
            if (threshold <= 0) return;

            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInKuudra()) return;
            String loc = tracker.getLocation();
            if (loc == null || !loc.contains("T5")) return;

            String text = ChatUtils.stripColor(msg.getString());
            if (!text.equals("[NPC] Elle: I knew you could do it!")) return;

            long now = ServerTick.getTime();
            if (cooldown > now) return;
            cooldown = now + 2000;

            int current = countArrow("TWILIGHT_ARROW_POISON");
            if (current >= threshold) return;

            ChatUtils.sendCommand("gfs Twilight Arrow Poison " + (threshold - current));
        });
    }

    private static int countArrow(String sbid) {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        int total = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);
            if (!stack.isEmpty() && sbid.equals(ItemUtils.getSkyblockId(stack))) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
