package top.babyzombie.addons.util;

import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.babyzombie.addons.module.dungeon.DailyRunsCounter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * One-time migration of legacy persistence file layouts into the unified data/ structure.
 * Safe to run on every startup: every step is a no-op when the old file is already gone.
 *
 * Old layout (pre-3.1):
 *   config/babyzombieaddons/chest_counter.json 等平铺文件        → data/
 *   config/babyzombieaddons/abiphone/ui_settings.json          → data/abiphone_ui.json
 *   config/babyzombieaddons/abiphone/&lt;uuid&gt;_&lt;profileId&gt;.json  → data/&lt;uuid&gt;/&lt;profileId&gt;/abiphone.json
 *   config/babyzombieaddons/data/&lt;uuid&gt;_&lt;profileId&gt;/          → data/&lt;uuid&gt;/&lt;profileId&gt;/ (两级嵌套)
 *   data/&lt;uuid&gt;_&lt;profileId&gt;/dungeon_daily.json              → 合并进 data/dungeon_daily.json (Map 按档案细分)
 */
public final class PersistenceMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger("BabyzombieAddons/PersistenceMigration");
    private static final Path CONFIG_ROOT = FabricLoader.getInstance().getConfigDir()
            .resolve("babyzombieaddons");

    private PersistenceMigration() {}

    /** Must run before any module reads its persisted data. */
    public static void run() {
        // 1. 配置根平铺文件 → data/ 根层
        DataPersistence.moveFromConfigRoot("chest_counter.json", null, "chest_counter.json");
        DataPersistence.moveFromConfigRoot("protected_items.json", null, "protected_items.json");
        DataPersistence.moveFromConfigRoot("ReheatedGummyPolarBear.json", null, "reheated_gummy_polar_bear.json");
        DataPersistence.moveFromConfigRoot("abiphone/ui_settings.json", null, "abiphone_ui.json");

        // 2. 按档案分的 abiphone 文件 → data/<uuid>/<profileId>/abiphone.json
        migrateAbiphoneProfiles();

        // 3. data/ 下旧扁平档案目录 → 两级嵌套,并合并 dungeon_daily.json
        migrateFlatProfileDirs();
    }

    private static void migrateAbiphoneProfiles() {
        Path abiphoneDir = CONFIG_ROOT.resolve("abiphone");
        if (!Files.isDirectory(abiphoneDir)) return;
        try (var stream = Files.list(abiphoneDir)) {
            for (Path p : stream.filter(Files::isRegularFile).toList()) {
                String name = p.getFileName().toString();
                if (!name.endsWith(".json") || name.equals("ui_settings.json")) continue;
                String[] parts = splitProfileKey(name.substring(0, name.length() - ".json".length()));
                if (parts == null) {
                    LOGGER.warn("Skipping unrecognized legacy file: {}", p);
                    continue;
                }
                DataPersistence.moveFromConfigRoot("abiphone/" + name, parts[0] + "/" + parts[1], "abiphone.json");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to migrate abiphone profile files", e);
        }
        // 文件全部搬走后,旧目录已空则一并清掉(还有别的文件就留着)
        try (var remaining = Files.list(abiphoneDir)) {
            if (remaining.findAny().isEmpty()) Files.deleteIfExists(abiphoneDir);
        } catch (IOException ignored) {}
    }

    private static void migrateFlatProfileDirs() {
        Path dataDir = CONFIG_ROOT.resolve("data");
        if (!Files.isDirectory(dataDir)) return;
        Map<String, DailyRunsCounter.DailyData> daily = new HashMap<>();
        try (var stream = Files.list(dataDir)) {
            for (Path dir : stream.filter(Files::isDirectory).toList()) {
                String name = dir.getFileName().toString();
                String[] parts = splitProfileKey(name);
                if (parts == null) continue; // 新两级结构(uuid 目录不含下划线)或无关目录
                // 合并 dungeon_daily.json 进单文件;读失败则保留原文件,不丢数据
                if (Files.exists(dir.resolve("dungeon_daily.json"))) {
                    DailyRunsCounter.DailyData d = DataPersistence.load(
                            name, "dungeon_daily.json", DailyRunsCounter.DailyData.class);
                    if (d != null) {
                        daily.put(name, d);
                        try {
                            Files.deleteIfExists(dir.resolve("dungeon_daily.json"));
                        } catch (IOException e) {
                            LOGGER.warn("Failed to delete legacy {}", dir.resolve("dungeon_daily.json"));
                        }
                    }
                }
                DataPersistence.moveDirectory(name, parts[0] + "/" + parts[1]);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to migrate flat profile directories", e);
        }
        if (!daily.isEmpty()) {
            Type type = new TypeToken<Map<String, DailyRunsCounter.DailyData>>(){}.getType();
            Map<String, DailyRunsCounter.DailyData> existing = DataPersistence.load("dungeon_daily.json", type);
            if (existing == null) existing = new HashMap<>();
            existing.putAll(daily);
            DataPersistence.save("dungeon_daily.json", existing);
        }
    }

    /** 按第一个下划线拆 "uuid_profileId"(标准 UUID 不含下划线,拆分稳定)。 */
    private static String[] splitProfileKey(String key) {
        int i = key.indexOf('_');
        if (i <= 0 || i >= key.length() - 1) return null;
        return new String[]{key.substring(0, i), key.substring(i + 1)};
    }
}
