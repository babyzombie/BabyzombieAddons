package top.babyzombie.addons.module.loadout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.babyzombie.addons.util.ChatUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * 从预设物品的 lore 反查物品库，获取原始 ItemStack。
 * 核心流程：lore 名称 → cleanName → 反向索引 → skyblockId → ItemStack
 */
public final class LoadoutItemResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger("BabyzombieAddons/LoadoutResolver");

    /** 保留 ASCII 字母/数字/空格/撇号/连字符，其余全部清掉 */
    private static final Pattern CLEAN_PATTERN = Pattern.compile("[^a-zA-Z0-9 '\\-]");

    /** 反向索引：cleanName → skyblockId */
    private static volatile Map<String, String> reverseIndex;

    /** 重铸前缀集合（小写） */
    private static volatile Set<String> reforgePrefixes;

    /** 构建中标志，防止重复触发 */
    private static boolean building = false;

    private LoadoutItemResolver() {}

    // ==================== 名字清洗 ====================

    /**
     * 清洗预设 lore 中的物品名称，使其能与物品库的 displayname 匹配。
     * 管道：stripColor → removeEmoji → 去装饰符号 → 去重铸前缀 → trim
     */
    public static String cleanName(String raw) {
        if (raw == null || raw.isEmpty()) return "";

        // 第 1 步：清 Minecraft 颜色码 §x 和 &x
        String name = ChatUtils.stripColor(raw);

        // 第 2 步：清 PUA 字符和 emoji（Hypixel 自定义图标、皮肤碎片等）
        name = ChatUtils.removeEmoji(name);

        // 第 3 步：清所有非 ASCII 字母/数字/空格/撇号/连字符的装饰符号
        name = CLEAN_PATTERN.matcher(name).replaceAll("");

        // 第 4 步：合并空格 + trim（必须在去前缀之前，否则前导空格导致匹配失败）
        name = name.replaceAll("\\s{2,}", " ").trim();

        // 第 5 步：去重铸前缀
        if (name.startsWith("Shiny ")) name = name.substring("Shiny ".length()).trim();
        name = stripReforgePrefix(name);
        return name.trim();
    }

    /**
     * 去除物品名开头的重铸前缀。
     * 动态从物品库加载前缀列表，优先匹配长的（如 "Greater Spook" 优先于 "Great"）。
     * 特例："Very"（Wise 重铸 + Wise Dragon Helmet → "Very Wise Dragon Helmet"）
     */
    private static String stripReforgePrefix(String name) {
        Set<String> prefixes = getReforgePrefixes();
        if (prefixes.isEmpty()) return name;

        List<String> sorted = new ArrayList<>(prefixes);
        sorted.add("Very");
        sorted.sort((a, b) -> Integer.compare(b.length(), a.length()));

        String lower = name.toLowerCase(Locale.ROOT);
        for (String prefix : sorted) {
            String prefixLower = prefix.toLowerCase(Locale.ROOT) + " ";
            if (lower.startsWith(prefixLower)) {
                return name.substring(prefix.length()).trim();
            }
        }
        return name;
    }

    // ==================== 重铸前缀加载 ====================

    private static Set<String> getReforgePrefixes() {
        if (reforgePrefixes != null) return reforgePrefixes;

        synchronized (LoadoutItemResolver.class) {
            if (reforgePrefixes != null) return reforgePrefixes;
            reforgePrefixes = loadReforgePrefixes();
            return reforgePrefixes;
        }
    }

    private static Set<String> loadReforgePrefixes() {
        Set<String> set = new HashSet<>();
        Path constantsDir = resolveConstantsDir();
        if (constantsDir == null) return set;

        // reforges.json — 铁砧重铸（key 就是 reforgeName）
        Path reforgesFile = constantsDir.resolve("reforges.json");
        if (Files.exists(reforgesFile)) {
            try {
                JsonObject obj = JsonParser.parseString(Files.readString(reforgesFile)).getAsJsonObject();
                set.addAll(obj.keySet());
            } catch (IOException e) {
                LOGGER.warn("Failed to read reforges.json", e);
            }
        }

        // reforgestones.json — 重铸石重铸（reforgeName 字段）
        Path stonesFile = constantsDir.resolve("reforgestones.json");
        if (Files.exists(stonesFile)) {
            try {
                JsonObject obj = JsonParser.parseString(Files.readString(stonesFile)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    JsonObject stone = entry.getValue().getAsJsonObject();
                    JsonElement refName = stone.get("reforgeName");
                    if (refName != null) {
                        set.add(refName.getAsString());
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to read reforgestones.json", e);
            }
        }
        return set;
    }

    // ==================== 反向索引 ====================

    /**
     * 触发反向索引的异步构建。幂等，多次调用只有第一次会启动构建。
     */
    /**
     * 确保索引已构建。如果为空则每次调用都重试（等 Skyblocker repo 就绪）。
     * 在打开 Loadout 页面时调用。
     */
    public static synchronized void ensureIndex() {
        if (building) return;
        building = true;
        CompletableFuture.runAsync(() -> {
            buildReverseIndex();
            if (reverseIndex != null && reverseIndex.isEmpty()) {
                LOGGER.warn("Reverse index still empty, will retry on next page open");
            }
        });
    }

    /** 通过 cleanName 查找 skyblockId。如果索引未就绪则返回 null。 */
    @Nullable
    public static String getSkyblockId(String cleanName) {
        Map<String, String> idx = reverseIndex;
        if (idx == null) return null;
        return idx.get(cleanName);
    }

    public static boolean isReady() {
        return reverseIndex != null;
    }

    public static int getIndexSize() {
        return reverseIndex != null ? reverseIndex.size() : 0;
    }

    /** 通过 skyblockId 从 Skyblocker 物品库获取完整 ItemStack */
    public static ItemStack createItemFromRepo(String skyblockId) {
        if (skyblockId == null) return ItemStack.EMPTY;
        try {
            var flex = de.hysky.skyblocker.skyblock.itemlist.ItemRepository.getItemStack(skyblockId);
            if (flex != null) {
                ItemStack stack = flex.getStackOrEmpty();
                if (!stack.isEmpty()) return stack;
            }
        } catch (Throwable t) { LOGGER.warn("Skyblocker getItemStack failed for {}", skyblockId, t); }
        return ItemStack.EMPTY;
    }

    private static void buildReverseIndex() {
        if (!FabricLoader.getInstance().isModLoaded("skyblocker")) {
            LOGGER.warn("Skyblocker not loaded, skipping reverse index");
            building = false; return;
        }
        try {
            var items = de.hysky.skyblocker.skyblock.itemlist.ItemRepository.getItems();
            if (items == null) { building = false; return; }
            Map<String, String> index = new HashMap<>();
            for (var item : items) {
                // 用 NEU name（对应文件名，如 GOLDEN_DRAGON;4）而非 getSkyblockId（宠物统一返回 "PET"）
                String id = item.getNeuName();
                if (id == null) continue;
                ItemStack stack = item.getStackOrEmpty();
                if (stack.isEmpty()) continue;
                String name = ChatUtils.stripColor(stack.getHoverName().getString()).trim();
                name = name.replaceAll("\\[Lvl [^]]+]\\s*", "");
                if (!name.isEmpty()) index.put(name, id);
            }
            reverseIndex = index;
            building = false;
            LOGGER.info("Built reverse index via Skyblocker: {} entries", index.size());
        } catch (Throwable t) {
            LOGGER.warn("Failed to build index from Skyblocker", t);
            building = false;
        }
    }

    // ==================== 路径解析 ====================

    @Nullable
    private static Path resolveRepoRoot() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path p = gameDir.resolve("config").resolve("skyblocker").resolve("item-repo");
        if (Files.isDirectory(p)) return p;
        p = gameDir.resolve(".firmament").resolve("repo-extracted");
        if (Files.isDirectory(p)) return p;
        p = gameDir.resolve("config").resolve("notenoughupdates").resolve("repo");
        if (Files.isDirectory(p)) return p;
        return null;
    }

    @Nullable
    private static Path resolveConstantsDir() {
        Path root = resolveRepoRoot();
        if (root == null) return null;
        Path constants = root.resolve("constants");
        return Files.isDirectory(constants) ? constants : null;
    }

    // ==================== 预设 Lore 解析 ====================

    /** 解析预设物品的 lore，提取防具、饰品、宠物、属性等信息 */
    public static PresetData parsePresetLore(ItemStack presetStack) {
        ItemLore lore = presetStack.get(DataComponents.LORE);
        if (lore == null) return PresetData.EMPTY;

        PresetData data = new PresetData();
        data.presetName = ChatUtils.stripColor(presetStack.getHoverName().getString());

        List<String> lines = new ArrayList<>();
        List<net.minecraft.network.chat.Component> rawLines = new ArrayList<>();
        for (var line : lore.lines()) {
            lines.add(line.getString());
            rawLines.add(line);
        }

        for (int li = 0; li < lines.size(); li++) {
            String line = lines.get(li);
            String stripped = ChatUtils.stripColor(line);
            if (stripped.isEmpty()) continue;

            // 防具
            if (stripped.startsWith("Helmet: ")) {
                data.helmetName = extractItemName(stripped, "Helmet: ");
            } else if (stripped.startsWith("Chestplate: ")) {
                data.chestplateName = extractItemName(stripped, "Chestplate: ");
            } else if (stripped.startsWith("Leggings: ")) {
                data.leggingsName = extractItemName(stripped, "Leggings: ");
            } else if (stripped.startsWith("Boots: ")) {
                data.bootsName = extractItemName(stripped, "Boots: ");
            }
            // 饰品
            else if (stripped.startsWith("Necklace: ")) {
                data.necklaceName = extractItemName(stripped, "Necklace: ");
            } else if (stripped.startsWith("Cloak: ")) {
                data.cloakName = extractItemName(stripped, "Cloak: ");
            } else if (stripped.startsWith("Belt: ")) {
                data.beltName = extractItemName(stripped, "Belt: ");
            } else if (stripped.startsWith("Gloves/Bracelet: ")) {
                data.glovesName = extractItemName(stripped, "Gloves/Bracelet: ");
            }
            // 宠物： "Pet: [Lvl X] [stats] PetName" → 提取等级和名字
            else if (stripped.startsWith("Pet: ")) {
                String petPart = stripped.substring(5).trim();
                // 提取等级
                var lvlMatcher = java.util.regex.Pattern.compile("\\[Lvl (\\d+)]").matcher(petPart);
                if (lvlMatcher.find()) data.petLevel = lvlMatcher.group(1);
                // 去掉 [Lvl X] 和 [...] 格式的前缀（cleanName 用）
                String cleaned = petPart.replaceAll("\\[.*?\\]", "").trim();
                if (!cleaned.isEmpty() && !cleaned.equalsIgnoreCase("None")) {
                    data.petName = cleaned;
                }
                // 同时保留带颜色代码的原始宠物名（仅去掉 "Pet: " 前缀）
                String legacy = ChatUtils.toLegacyString(rawLines.get(li));
                String rawName = legacy.replaceFirst("^.*Pet:\\s*", "").trim();
                if (!rawName.isEmpty() && !ChatUtils.stripColor(rawName).equalsIgnoreCase("None")) {
                    data.petNameRaw = rawName;
                }
            }
            // 属性
            else if (stripped.startsWith("Power Stone: ")) {
                data.powerStone = stripped.substring(13).trim();
            } else if (stripped.startsWith("Tuning Template Slot: ")) {
                data.tuningSlot = stripped.substring(22).trim();
            } else if (stripped.startsWith("HOTM: ")) {
                data.hotm = stripped.substring(6).trim();
            } else if (stripped.startsWith("HOTF: ")) {
                data.hotf = stripped.substring(6).trim();
            }
        }

        return data;
    }

    /** 从 lore 行提取物品名并清洗 */
    private static String extractItemName(String line, String prefix) {
        String raw = line.substring(prefix.length()).trim();
        if (raw.isEmpty() || raw.equalsIgnoreCase("None") || raw.equalsIgnoreCase("Empty")) return null;
        return cleanName(raw);
    }

    // ==================== 数据结构 ====================

    public static class PresetData {
        public static final PresetData EMPTY = new PresetData();

        public String presetName;
        public String helmetName, chestplateName, leggingsName, bootsName;
        public String necklaceName, cloakName, beltName, glovesName;
        public String petName, petNameRaw, petLevel;
        public String powerStone, tuningSlot, hotm, hotf;

        public boolean isEmpty() {
            return helmetName == null && chestplateName == null && leggingsName == null
                && bootsName == null && necklaceName == null && cloakName == null
                && beltName == null && glovesName == null && petName == null;
        }

        public boolean hasAnyArmor() {
            return helmetName != null || chestplateName != null
                || leggingsName != null || bootsName != null;
        }

        public boolean hasAnyEquipment() {
            return necklaceName != null || cloakName != null
                || beltName != null || glovesName != null;
        }

        public String[] getArmorNames() {
            return new String[]{ helmetName, chestplateName, leggingsName, bootsName };
        }

        public String[] getEquipmentNames() {
            return new String[]{ necklaceName, cloakName, beltName, glovesName };
        }
    }
}
