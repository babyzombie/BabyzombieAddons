package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.ModConfig.ToxicArrowMinTier;
import top.babyzombie.addons.config.ModConfig.ToxicArrowTiming;
import top.babyzombie.addons.config.ModConfig.TwilightArrowTiming;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.Scheduler;

public final class ArrowPoisonRefill {

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

            boolean toxicMatches = cfg.phase3.arrowPoison.toxicArrowThreshold > 0
                    && atLeastTier(loc, cfg.phase3.arrowPoison.toxicArrowMinTier)
                    && matchesToxicTiming(text, cfg.phase3.arrowPoison.toxicArrowTiming);

            boolean twilightMatches = cfg.phase3.arrowPoison.twilightArrowThreshold > 0
                    && inT5
                    && matchesTwilightTiming(text, cfg.phase3.arrowPoison.twilightArrowTiming);

            if (!toxicMatches && !twilightMatches) return;

            long now = System.currentTimeMillis();

            if (toxicMatches && toxicCooldown <= now && !"p3".equals(KuudraLocationTracker.area)) {
                int current = countArrow("TOXIC_ARROW_POISON");
                int target = cfg.phase3.arrowPoison.toxicArrowThreshold;
                if (cfg.phase3.arrowPoison.toxicArrowPerMissing > 0) {
                    int nearbyShooters = countNearbyTerminatorShooters();
                    int missing = Math.max(0, 2 - nearbyShooters);
                    target += missing * cfg.phase3.arrowPoison.toxicArrowPerMissing;
                }
                if (current < target) {
                    ChatUtils.sendCommand("gfs Toxic Arrow Poison " + (target - current));
                    toxicCooldown = now + 2000;
                    if (twilightMatches) {
                        int threshold = cfg.phase3.arrowPoison.twilightArrowThreshold;
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
                if (current < cfg.phase3.arrowPoison.twilightArrowThreshold) {
                    ChatUtils.sendCommand("gfs Twilight Arrow Poison " + (cfg.phase3.arrowPoison.twilightArrowThreshold - current));
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
            case STUNNER_ENTER -> KuudraChatLines.isEatenByKuudra(text);
            case KUUDRA_START -> KuudraChatLines.isFishUpKuudra(text);
            case SUPPLIES_DONE -> KuudraChatLines.isSuppliesCollected(text);
            case BALLISTA_READY -> KuudraChatLines.isBallistaReady(text);
            case KUUDRA_STUNNED -> KuudraChatLines.isDestroyedPod(text);
        };
    }

    private static boolean matchesTwilightTiming(String text, TwilightArrowTiming timing) {
        return switch (timing) {
            case P4_SHORTLY_AFTER -> KuudraChatLines.isKnewYouCouldDoIt(text);
            case KUUDRA_START -> KuudraChatLines.isFishUpKuudra(text);
            case SUPPLIES_DONE -> KuudraChatLines.isSuppliesCollected(text);
            case BALLISTA_READY -> KuudraChatLines.isBallistaReady(text);
            case KUUDRA_STUNNED -> KuudraChatLines.isDestroyedPod(text);
            case P4_START -> KuudraChatLines.isP4Start(text);
            case P4_TRUE_LAIR -> KuudraChatLines.isTrueLair(text);
        };
    }

    /**
     * 统计周围 20 格内手持 Terminator 的非 NPC 玩家数量（不含自己）。
     */
    private static int countNearbyTerminatorShooters() {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        var level = player.level();
        var box = new AABB(
                player.getX() - 20, player.getY() - 20, player.getZ() - 20,
                player.getX() + 20, player.getY() + 20, player.getZ() + 20);
        int count = 0;
        for (var p : level.getEntitiesOfClass(Player.class, box, p -> p != player)) {
            // 名字里有空格的是 NPC
            if (p.getName().getString().contains(" ")) continue;
            var item = p.getMainHandItem();
            if (item.isEmpty()) continue;
            // 先看是不是弓，再看 SkyBlock ID 是不是 TERMINATOR
            if (!(item.getItem() instanceof BowItem)) continue;
            if (!"TERMINATOR".equals(ItemUtils.getSkyblockId(item))) continue;
            count++;
        }
        return count;
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
