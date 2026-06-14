package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfig.ToxicArrowMinTier;
import top.babyzombie.addons.config.ModConfig.ToxicArrowTiming;
import top.babyzombie.addons.config.ModConfig.TwilightArrowTiming;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.Scheduler;

import java.util.regex.Pattern;

public final class ArrowPoisonRefill {

    private static final Pattern EATEN_BY_KUUDRA = Pattern.compile(
            "([0-9a-zA-Z_]{2,24}) has been eaten by Kuudra!");
    private static final Pattern DESTROYED_POD = Pattern.compile(
            "([0-9a-zA-Z_]{2,24}) destroyed one of Kuudra's pods");

    private static long toxicCooldown;
    private static long twilightCooldown;

    private ArrowPoisonRefill() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(msg.getString());

            var cfg = ModConfigManager.get().kuudra;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInKuudra()) return;

            String loc = tracker.getLocation();
            boolean inT5 = loc != null && loc.contains("T5");

            boolean toxicMatches = cfg.toxicArrowThreshold > 0
                    && atLeastTier(loc, cfg.toxicArrowMinTier)
                    && matchesToxicTiming(text, cfg.toxicArrowTiming);

            boolean twilightMatches = cfg.twilightArrowThreshold > 0
                    && inT5
                    && matchesTwilightTiming(text, cfg.twilightArrowTiming);

            if (!toxicMatches && !twilightMatches) return;

            long now = System.currentTimeMillis();

            if (toxicMatches && toxicCooldown <= now && !"p3".equals(KuudraLocationTracker.area)) {
                int current = countArrow("TOXIC_ARROW_POISON");
                if (current < cfg.toxicArrowThreshold) {
                    ChatUtils.sendCommand("gfs Toxic Arrow Poison " + (cfg.toxicArrowThreshold - current));
                    toxicCooldown = now + 2000;
                    if (twilightMatches) {
                        int threshold = cfg.twilightArrowThreshold;
                        Scheduler.schedule(40, () -> {
                            int cur = countArrow("TWILIGHT_ARROW_POISON");
                            if (cur < threshold) {
                                ChatUtils.sendCommand("gfs Twilight Arrow Poison " + (threshold - cur));
                                twilightCooldown = System.currentTimeMillis() + 2000;
                            }
                        });
                        twilightCooldown = now + 4000;
                    }
                }
            }

            if (twilightMatches && twilightCooldown <= now) {
                int current = countArrow("TWILIGHT_ARROW_POISON");
                if (current < cfg.twilightArrowThreshold) {
                    ChatUtils.sendCommand("gfs Twilight Arrow Poison " + (cfg.twilightArrowThreshold - current));
                    twilightCooldown = now + 2000;
                }
            }
        });
    }

    private static boolean atLeastTier(String loc, ToxicArrowMinTier minTier) {
        if (loc == null) return false;
        int tier = extractTier(loc);
        int min = minTier.ordinal() + 1;
        return tier >= min;
    }

    private static int extractTier(String loc) {
        for (int t = 5; t >= 1; t--) {
            if (loc.contains("T" + t)) return t;
        }
        return 0;
    }

    private static boolean matchesToxicTiming(String text, ToxicArrowTiming timing) {
        return switch (timing) {
            case STUNNER_ENTER -> EATEN_BY_KUUDRA.matcher(text).find();
            case KUUDRA_START -> text.contains("Okay adventurers, I will go and fish up Kuudra");
            case SUPPLIES_DONE -> text.equals("[NPC] Elle: OMG! Great work collecting my supplies!");
            case BALLISTA_READY -> text.equals("[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!")
                    || text.equals("[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blow!");
            case KUUDRA_STUNNED -> DESTROYED_POD.matcher(text).find();
        };
    }

    private static boolean matchesTwilightTiming(String text, TwilightArrowTiming timing) {
        return switch (timing) {
            case P4_SHORTLY_AFTER -> text.equals("[NPC] Elle: I knew you could do it!");
            case KUUDRA_START -> text.contains("Okay adventurers, I will go and fish up Kuudra");
            case SUPPLIES_DONE -> text.equals("[NPC] Elle: OMG! Great work collecting my supplies!");
            case BALLISTA_READY -> text.equals("[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!")
                    || text.equals("[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blow!");
            case KUUDRA_STUNNED -> DESTROYED_POD.matcher(text).find();
            case P4_START -> text.equals("[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!");
            case P4_TRUE_LAIR -> text.equals("[NPC] Elle: What just happened? Is this Kuudra's real lair?");
        };
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
