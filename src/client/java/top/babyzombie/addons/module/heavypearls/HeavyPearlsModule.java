package top.babyzombie.addons.module.heavypearls;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.Scheduler;

import java.util.HashMap;
import java.util.Map;

/**
 * Daily heavy pearls reminder. Reminds on world join if not yet collected.
 */
public final class HeavyPearlsModule {

    private static final Map<String, Integer> collectedDays = new HashMap<>();
    private static final Map<String, Boolean> shownToday = new HashMap<>();

    private HeavyPearlsModule() {}

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Scheduler.schedule(60, () -> {
                var tracker = HypixelLocationTracker.getInstance();
                if (!tracker.isInSkyblock()) return;
                String profileId = tracker.getProfileId();
                if (profileId == null) return;

                int today = todayKey();
                if (Boolean.TRUE.equals(shownToday.get(profileId))) return;
                Integer collected = collectedDays.get(profileId);
                if (collected != null && collected >= today) return;

                shownToday.put(profileId, true);
                ChatUtils.sendCommand("pc [BZA] Time to collect today's Heavy Pearls!");
                var player = Minecraft.getInstance().player;
                if (player != null)
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§6§lTime to collect Heavy Pearls!"), true);
            });
        });

        // Detect collection
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = message.getString();
            if (text.contains("Heavy Pearls") && text.contains("Available: 0")) {
                String profileId = HypixelLocationTracker.getInstance().getProfileId();
                if (profileId != null) collectedDays.put(profileId, todayKey());
            }
        });
    }

    private static int todayKey() {
        var now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);
        return now.getYear() * 10000 + now.getMonthValue() * 100 + now.getDayOfMonth();
    }
}
