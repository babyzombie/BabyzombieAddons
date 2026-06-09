package top.babyzombie.addons.module.dungeon;

import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.DataPersistence;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * Tracks and persists daily dungeon run counts per profile.
 */
public final class DailyRunsCounter {

    private static int dailyRuns, dailyTimestamp;

    private DailyRunsCounter() {}

    public static void incrementAndShow() {
        var mode = ModConfigManager.get().dungeon.dailyRunsCounter;
        if (mode == ModConfig.DailyCounterMode.OFF) return;
        loadDaily();
        dailyRuns++;
        saveDaily();
        if (mode == ModConfig.DailyCounterMode.ALWAYS || dailyRuns <= 5) {
            String color = dailyRuns <= 5 ? "§a" : "§e";
            ChatUtils.showMessage(
                Component.translatable("babyzombieaddons.dailyRuns.info", color + dailyRuns).getString()
            );
        }
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
