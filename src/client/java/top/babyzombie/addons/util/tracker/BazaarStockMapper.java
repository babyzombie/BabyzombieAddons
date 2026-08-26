package top.babyzombie.addons.util.tracker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.babyzombie.addons.util.ChatUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import top.babyzombie.addons.util.pet.RomanNumeral;

/**
 * 游戏内物品 id → Bazaar 产品 id 的映射（三路回退读 NEU 物品库）。
 *
 * <p>数据源：{@code constants/bazaarstocks.json}（NEU 物品库，skyblocker / firmament / NEU mod
 * 都会拉取，本地基本必有）。格式：{@code [{"stock": "ENCHANTMENT_SCAVENGER_4", "id": "SCAVENGER;4"}, ...]}，
 * 覆盖附魔书（{@code 名;等级}）和属性碎片（{@code ATTRIBUTE_SHARD_XXX;1}）这类物品 id ≠ bazaar 产品 id 的条目。
 *
 * <p>普通物品（物品 id 不含分号）无需映射：物品 id 即 bazaar 产品 id。
 *
 * <p>三路回退顺序与 {@link top.babyzombie.addons.util.pet.PetConstants} 一致：
 * skyblocker → firmament → NEU。
 *
 * <p>附魔书显示名反查：书页 lore 里的附魔名可能与内部 id 不一致
 * （如 "Vampiric Vitality V" ↔ {@code MANA_VAMPIRE;5} ↔ {@code ENCHANTMENT_MANA_VAMPIRE_5}），
 * 惰性扫描 {@code items/*;*.json} 的 lore 建 "显示名(归一) → bazaar 产品 id" 反查表
 * （与 skyblocker EnchantedBookUtils.getApiIdByName 同款方案）。
 */
final class BazaarStockMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger("BabyzombieAddons/BazaarStockMapper");

    private static BazaarStockMapper INSTANCE;
    private boolean loaded;

    /** 反向索引：物品 id（大写归一）→ bazaar 产品 id（stock） */
    private Map<String, String> idToStock = Map.of();
    /** 属性碎片显示名（strip 颜色码后）→ bazaar 产品 id；惰性加载 */
    private volatile Map<String, String> shardNameToStock = Map.of();
    private boolean shardIndexLoaded;
    /** 附魔书显示名（归一后，如 VAMPIRIC_VITALITY_5）→ bazaar 产品 id；惰性加载 */
    private volatile Map<String, String> enchantDisplayToStock = Map.of();
    private boolean enchantDisplayIndexLoaded;

    private BazaarStockMapper() {}

    public static BazaarStockMapper getInstance() {
        if (INSTANCE == null) INSTANCE = new BazaarStockMapper();
        return INSTANCE;
    }

    /** 惰性加载；找不到任何物品库时返回 false（功能不可用，调用方自行降级） */
    public boolean ensureLoaded() {
        if (loaded) return true;
        Path root = resolveItemRepo();
        if (root == null) {
            LOGGER.warn("[BazaarStockMapper] No item repo found (skyblocker/firmament/NEU)");
            return false;
        }
        Path stockJson = root.resolve("constants").resolve("bazaarstocks.json");
        if (!Files.exists(stockJson)) {
            LOGGER.warn("[BazaarStockMapper] bazaarstocks.json missing in {}", root);
            return false;
        }
        try {
            String raw = Files.readString(stockJson);
            JsonArray arr = JsonParser.parseString(raw).getAsJsonArray();
            Map<String, String> map = new HashMap<>(arr.size() * 2);
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                String stock = o.get("stock").getAsString();
                String id = o.get("id").getAsString();
                if (stock != null && id != null) {
                    map.put(id.toUpperCase(Locale.ROOT), stock);
                }
            }
            idToStock = Map.copyOf(map);
            loaded = true;
            return true;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("[BazaarStockMapper] Failed to parse bazaarstocks.json", e);
            return false;
        }
    }

    /**
     * 物品 id → bazaar 产品 id。普通物品直接返回自身；
     * 附魔书/属性碎片（id 带分号）查反向索引；查不到返回 null。
     */
    @Nullable
    public String lookupProductId(String itemId) {
        if (itemId == null || itemId.isBlank()) return null;
        String normalized = itemId.trim().toUpperCase(Locale.ROOT);
        if (!normalized.contains(";")) return normalized; // 普通物品：物品 id 即产品 id
        if (!ensureLoaded()) return null;
        return idToStock.get(normalized);
    }

    /** @return 是否已成功加载映射表 */
    public boolean isLoaded() { return loaded; }

    /** @return productId 是否为已知的 bazaar 产品（在 bazaarstocks 的 stock 集合中） */
    public boolean isKnownStock(String productId) {
        if (productId == null) return false;
        if (!ensureLoaded()) return false;
        return idToStock.containsValue(productId);
    }

    /**
     * 附魔书"核心名结尾匹配"兜底：处理显示名缺前缀的附魔（ultimate 系显示为
     * {@code [Crop Fever V]}，索引里是 {@code ULTIMATE_CROP_FEVER;5}）。
     * 同时匹配核心名和等级，唯一命中才返回，多命中/无命中返回 null。
     * 数据源于索引本身，不会因 enchants.json 等外部表过时而漏新附魔。
     */
    @Nullable
    public String lookupEnchantByCore(String coreName, int level) {
        if (!ensureLoaded()) return null;
        String suffix = coreName.toUpperCase(Locale.ROOT);
        String lvl = Integer.toString(level);
        String matched = null;
        for (Map.Entry<String, String> e : idToStock.entrySet()) {
            String id = e.getKey();
            int semi = id.indexOf(';');
            if (semi <= 0) continue;
            String name = id.substring(0, semi);
            String idLevel = id.substring(semi + 1);
            if (name.endsWith(suffix) && lvl.equals(idLevel)) {
                if (matched != null) return null; // 多命中：歧义，不猜
                matched = e.getValue();
            }
        }
        return matched;
    }

    /**
     * 属性碎片显示名反查（head 类物品无 NBT id，只能靠显示名）。
     * NEU items/ 里 {@code ATTRIBUTE_SHARD_*;1.json} 的 displayname（strip 颜色码）
     * 即游戏内显示名（如 "Mist Shard"）。惰性建索引，首次查询时遍历 items 目录。
     */
    @Nullable
    public String lookupByDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) return null;
        if (!ensureShardIndex()) return null;
        return shardNameToStock.get(displayName.trim());
    }

    private boolean ensureShardIndex() {
        if (shardIndexLoaded) return true;
        Path root = resolveItemRepo();
        if (root == null) return false;
        Path itemsDir = root.resolve("items");
        if (!Files.isDirectory(itemsDir)) return false;
        Map<String, String> map = new HashMap<>();
        try (var stream = Files.list(itemsDir)) {
            for (Path f : stream.filter(p -> p.getFileName().toString().contains(";")).toList()) {
                String fileName = f.getFileName().toString();
                if (!fileName.startsWith("ATTRIBUTE_SHARD_")) continue;
                try {
                    JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                    String display = ChatUtils.stripColor(o.get("displayname").getAsString()).trim();
                    if (display.isBlank()) continue;
                    // internalname 才是权威物品 id（如 ATTRIBUTE_SHARD_SOLAR_POWER;1），
                    // 与文件名一致但以字段为准，避免命名差异
                    if (!o.has("internalname")) continue;
                    String stock = lookupProductId(o.get("internalname").getAsString());
                    if (stock != null) map.put(display, stock);
                } catch (IOException | RuntimeException ignored) {
                }
            }
        } catch (IOException e) {
            LOGGER.warn("[BazaarStockMapper] Failed to list items dir", e);
            return false;
        }
        shardNameToStock = Map.copyOf(map);
        shardIndexLoaded = true;
        return true;
    }

    /** 三路回退定位物品库根目录：skyblocker → firmament → NEU mod */
    @Nullable
    private static Path resolveItemRepo() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path p = gameDir.resolve("config").resolve("skyblocker").resolve("item-repo");
        if (Files.isDirectory(p)) return p;
        p = gameDir.resolve(".firmament").resolve("repo-extracted");
        if (Files.isDirectory(p)) return p;
        p = gameDir.resolve("config").resolve("notenoughupdates").resolve("repo");
        if (Files.isDirectory(p)) return p;
        return null;
    }

    // ===== 附魔书显示名反查（lore 名 ≠ 内部 id 的附魔，如 "Vampiric Vitality V" ↔ MANA_VAMPIRE;5） =====

    /**
     * 附魔书显示名（如 "Vampiric Vitality V" / "Crop Fever V"）→ bazaar 产品 id。
     * 归一后查由书页 lore 惰性构建的反查表；查不到返回 null。
     */
    @Nullable
    public String lookupByEnchantDisplay(String displayName) {
        if (displayName == null || displayName.isBlank()) return null;
        if (!ensureEnchantDisplayIndex()) return null;
        return enchantDisplayToStock.get(normalizeEnchantDisplayName(displayName));
    }

    /** 附魔显示名归一（skyblocker 同款算法）：去色码/括号、大写、空格连字符→下划线、末位罗马数字→十进制 */
    @Nullable
    static String normalizeEnchantDisplayName(String name) {
        if (name == null) return null;
        String s = ChatUtils.stripColor(name);
        if (s == null) return null;
        s = s.trim().toUpperCase(Locale.ROOT);
        s = s.replace("[", "").replace("]", "").replace("(", "").replace(")", "");
        s = s.replaceAll("[\\s-]+", "_");
        int last = s.lastIndexOf('_');
        if (last > 0) {
            int lvl = RomanNumeral.parse(s.substring(last + 1));
            if (lvl >= 1 && lvl <= 10) {
                s = s.substring(0, last) + "_" + lvl;
            }
        }
        return s.isEmpty() ? null : s;
    }

    /**
     * 惰性构建附魔书显示名 → 产品 id 反查表：遍历 items 目录里带分号的文件，
     * 取书页 lore 的附魔名行（首个非空且不含 "Combinable in Anvil" 的行），
     * 归一后映射到该 id 对应的 bazaar 产品。同一 key 多个产品时保留首个。
     */
    private boolean ensureEnchantDisplayIndex() {
        if (enchantDisplayIndexLoaded) return true;
        if (!ensureLoaded()) return false; // 需要 idToStock（id → 产品）已加载
        Path root = resolveItemRepo();
        if (root == null) return false;
        Path itemsDir = root.resolve("items");
        if (!Files.isDirectory(itemsDir)) return false;
        Map<String, String> map = new HashMap<>();
        try (var stream = Files.list(itemsDir)) {
            for (Path f : stream.filter(p -> p.getFileName().toString().contains(";")).toList()) {
                String fileName = f.getFileName().toString();
                if (!fileName.endsWith(".json")) continue;
                String id = fileName.substring(0, fileName.length() - ".json".length());
                String stock = idToStock.get(id.toUpperCase(Locale.ROOT));
                if (stock == null || !stock.startsWith("ENCHANTMENT_")) continue;
                try {
                    JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                    String enchantName = extractEnchantDisplayName(o);
                    if (enchantName == null) continue;
                    String key = normalizeEnchantDisplayName(enchantName);
                    if (key == null || map.containsKey(key)) continue;
                    map.put(key, stock);
                } catch (IOException | RuntimeException ignored) {
                }
            }
        } catch (IOException e) {
            LOGGER.warn("[BazaarStockMapper] Failed to list items dir for enchant display index", e);
            return false;
        }
        enchantDisplayToStock = Map.copyOf(map);
        enchantDisplayIndexLoaded = true;
        return true;
    }

    /** 书页 lore 中的附魔显示名行：首个非空且不含 "Combinable in Anvil" 的行 */
    @Nullable
    private static String extractEnchantDisplayName(JsonObject item) {
        if (!item.has("lore") || !item.get("lore").isJsonArray()) return null;
        for (JsonElement el : item.getAsJsonArray("lore")) {
            String s = ChatUtils.stripColor(el.getAsString());
            if (s == null) continue;
            String t = s.trim();
            if (t.isEmpty()) continue;
            if (t.contains("Combinable in Anvil")) continue;
            return t;
        }
        return null;
    }
}
