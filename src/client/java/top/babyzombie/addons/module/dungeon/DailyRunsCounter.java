package top.babyzombie.addons.module.dungeon;

import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.DataPersistence;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks and persists daily dungeon run counts per profile.
 * Persisted as data/dungeon_daily.json, keyed by "uuid_profileId".
 */
public final class DailyRunsCounter {

    private static int dailyRuns, dailyTimestamp;
    private static final Map<String, DailyData> allData = new HashMap<>();

    private DailyRunsCounter() {}

    public static void incrementAndShow() {
        var mode = ModConfigManager.get().dungeon.dailyRunsCounter;
        if (mode == ModConfig.DailyCounterMode.OFF) return;
        if (HypixelLocationTracker.getInstance().isInAlpha()) return;
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

    private static String profileKey() {
        var t = HypixelLocationTracker.getInstance();
        return (t.getUuid() != null ? t.getUuid() : "unknown")
                + "_" + (t.getProfileId() != null ? t.getProfileId() : "unknown");
    }

    private static void loadDaily() {
        var type = new TypeToken<Map<String, DailyData>>(){}.getType();
        Map<String, DailyData> loaded = DataPersistence.load("dungeon_daily.json", type);
        if (loaded != null) {
            allData.clear();
            allData.putAll(loaded);
        }
        DailyData data = allData.get(profileKey());
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
        allData.put(profileKey(), new DailyData(dailyRuns, dailyTimestamp));
        DataPersistence.save("dungeon_daily.json", allData);
    }

    private static int todayKey() {
        var now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);
        return now.getYear() * 10000 + now.getMonthValue() * 100 + now.getDayOfMonth();
    }

    public record DailyData(int runs, int timestamp) {}
}
