package top.babyzombie.addons.module.dungeon;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.party.PartyModule;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.DataPersistence;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.Scheduler;

/**
 * Dungeon: F4 crowd hiding, auto requeue, auto chest close, daily counter.
 * End detection matches original ChatTriggers JS exact message patterns.
 */
public final class DungeonModule {

    private static int dailyRuns, dailyTimestamp;
    private static boolean instanceStarted;

    private DungeonModule() {}

    public static void init() {
        AutoRequeue.init();

        // Instance start
        ClientReceiveMessageEvents.GAME.register((m, o) -> {
            if (o) return;
            String t = ChatUtils.stripColor(m.getString());
            if (t.equals("Starting in 1 second.")) {
                instanceStarted = true;
                AutoRequeue.cancelAutoJoin = false;
                AutoRequeue.ended = false;
            }
        });

        // Instance end detection — exact message matching from original JS
        ClientReceiveMessageEvents.GAME.register((m, o) -> {
            if (o || !instanceStarted) return;
            String t = ChatUtils.stripColor(m.getString());
            // Exact message matching (space counts are fixed by Hypixel format)
            boolean win = t.equals("          "
                    + "          "
                    + "         > EXTRA STATS <")  // 29 spaces — dungeon win
                    || t.equals("          "
                    + "               "
                    + "      KUUDRA DOWN!");  // 31 spaces — kuudra win
            boolean fail = t.equals("          "
                    + "               "
                    + "               "
                    + " DEFEAT")  // 35 spaces — kuudra fail
                    || (t.startsWith("     ") && t.contains("   ☠ Defeated ") && t.contains(" in "));

            if (win || fail) {
                instanceStarted = false;

                if (win && ModConfigManager.get().dungeon.dailyCounter) {
                    loadDaily();
                    dailyRuns++;
                    saveDaily();
                    if (dailyRuns <= 5)
                        ChatUtils.sendCommand("pc Daily runs: " + dailyRuns + "/5");
                }

                AutoRequeue.schedule(win);
            }
        });

        // Cancel keywords from party chat (verified by PARTY_CHAT regex)
        ClientReceiveMessageEvents.GAME.register((m, o) -> {
            if (o) return;
            if (ModConfigManager.get().dungeon.autoRequeue == ModConfig.RequeueMode.OFF) return;
            // Only process party chat messages
            var pm = PartyModule.PARTY_CHAT.matcher(m.getString());
            if (!pm.find()) return;
            String t = ChatUtils.stripColor(pm.group(5)).trim().toLowerCase();
            for (String kw : ModConfigManager.get().dungeon.requeueCancelKeywords.toLowerCase().split("\\|")) {
                if (!kw.isEmpty() && t.equals(kw)) {
                    AutoRequeue.cancel();
                    return;
                }
            }
        });

        // Auto chest close
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (!ModConfigManager.get().dungeon.autoChestClose) return;
            if (!HypixelLocationTracker.getInstance().isInDungeon()) return;
            if (screen instanceof AbstractContainerScreen<?> cs) {
                String title = cs.getTitle().getString();
                if (title.contains("Chest") || title.contains("箱子")) {
                    Scheduler.schedule(4, () -> {
                        if (client.screen == screen && HypixelLocationTracker.getInstance().isInDungeon())
                            client.execute(() -> { if (client.player != null) client.player.closeContainer(); });
                    });
                }
            }
        });
    }

    private static String dailySubDir() {
        var t = HypixelLocationTracker.getInstance();
        return (t.getUuid() != null ? t.getUuid() : "unknown")
                + "_" + (t.getProfileId() != null ? t.getProfileId() : "unknown");
    }

    private static void loadDaily() {
        var data = DataPersistence.load(dailySubDir(), "dungeon_daily.json", DailyData.class);
        int today = todayKey();
        if (data != null && data.timestamp == today) {
            dailyRuns = data.runs;
            dailyTimestamp = data.timestamp;
        } else {
            dailyRuns = 0;
            dailyTimestamp = today;
        }
    }

    private static void saveDaily() {
        DataPersistence.save(dailySubDir(), "dungeon_daily.json",
                new DailyData(dailyRuns, dailyTimestamp));
    }

    private static int todayKey() {
        var now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);
        return now.getYear() * 10000 + now.getMonthValue() * 100 + now.getDayOfMonth();
    }

    private record DailyData(int runs, int timestamp) {}
}
