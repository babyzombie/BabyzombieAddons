package top.babyzombie.addons.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.serialization.JsonOps;
import de.hysky.skyblocker.injected.SkyblockerStack;
import de.hysky.skyblocker.skyblock.item.tooltip.info.TooltipInfoType;
import de.hysky.skyblocker.skyblock.itemlist.ItemRepository;
import de.hysky.skyblocker.utils.BazaarProduct;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class ItemUtils {
    private ItemUtils() {}

    /**
     * Extracts the SkyBlock item ID from an ItemStack's custom_data.
     * In 1.21, the id is stored directly in the root compound (no ExtraAttributes wrapper).
     */
    public static String getSkyblockId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        var tag = customData.copyTag();
        // 1.21 format: id directly in root
        String id = tag.getString("id").orElse(null);
        if (id != null) return id;
        // Fallback for legacy items: ExtraAttributes.id
        var extra = tag.getCompound("ExtraAttributes").orElse(null);
        if (extra == null) return null;
        return extra.getString("id").orElse(null);
    }

    /** 宠物稀有度 → item-repo 宠物条目序号（ENDERMAN;0~5.json） */
    private static final Map<String, Integer> PET_TIER_INDEX = Map.of(
            "COMMON", 0,
            "UNCOMMON", 1,
            "RARE", 2,
            "EPIC", 3,
            "LEGENDARY", 4,
            "MYTHIC", 5
    );

    /**
     * 解析可对得上 NEU 本地物品库名字（item-repo internalname / 文件名）的物品唯一 id。
     * <p>优先走 Skyblocker 运行时注入的 {@link SkyblockerStack#getNeuName()}（自带缓存，
     * 且覆盖 hat / 实验台等边缘规则）；Skyblocker 未安装（接口缺失或返回空）时回退到本地
     * 合成，规则与 Skyblocker 的 ItemUtils.getNeuId 一致。
     *
     * @return NEU 本地物品库名字；无法解析时返回 null
     */
    @Nullable
    public static String getNeuName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        try {
            String skyblockerName = ((SkyblockerStack) (Object) stack).getNeuName();
            if (skyblockerName != null && !skyblockerName.isEmpty()) return skyblockerName;
        } catch (NoClassDefFoundError e) {
            // Skyblocker 未安装：回退本地合成
        }
        return resolveNeuNameLocal(stack);
    }

    /**
     * 本地合成 NEU 物品库名字（Skyblocker 未安装时的兜底）：
     * <ul>
     *   <li>普通物品       → custom_data.id（如 HYPERION）</li>
     *   <li>PET            → petInfo 的 type;稀有度序号（如 ENDERMAN;0）</li>
     *   <li>RUNE           → runes 映射合成（如 MUSIC_RUNE;3）</li>
     *   <li>POTION         → potion 字段合成（如 POTION_ABSORPTION;3）</li>
     *   <li>ATTRIBUTE_SHARD → attributes 映射合成（如 ATTRIBUTE_SHARD_ARCANE_FORTUNE;1）</li>
     *   <li>ENCHANTED_BOOK → enchantments 映射合成（如 SHARPNESS;6、ULTIMATE_WISE;5）</li>
     * </ul>
     */
    @Nullable
    private static String resolveNeuNameLocal(ItemStack stack) {
        String id = getSkyblockId(stack);
        if (id == null || id.isEmpty()) return null;
        JsonObject tag = customDataJson(stack);
        if (tag == null) return id;
        return switch (id) {
            case "PET" -> resolvePetInternalName(tag);
            case "RUNE" -> resolveMapInternalName(tag, "runes", "", "_RUNE;");
            case "POTION" -> resolvePotionInternalName(tag);
            case "ATTRIBUTE_SHARD" -> resolveMapInternalName(tag, "attributes", "ATTRIBUTE_SHARD_", ";");
            case "ENCHANTED_BOOK" -> resolveMapInternalName(tag, "enchantments", "", ";");
            default -> id;
        };
    }

    // ===== NEU 物品库 → ItemStack =====

    /** 合法 NEU 物品库名字:大写字母/数字/下划线,可带 ;等级 后缀(防路径穿越) */
    private static final Pattern NEU_NAME_PATTERN = Pattern.compile("[A-Z0-9_]+(?:;\\d+)?");
    private static final int REPO_CACHE_SIZE = 64;
    private static final Map<String, JsonObject> REPO_ITEM_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, JsonObject> eldest) {
                    return size() > REPO_CACHE_SIZE;
                }
            });

    /**
     * 按 NEU 物品库名字取展示用 ItemStack。
     * <p>优先 Skyblocker 启动时加载的内存物品库(零 IO);未安装/未就绪时回退本地
     * item-repo 文件(config/skyblocker/item-repo/items),首次命中会有短暂卡顿,之后走缓存。
     *
     * @return 展示用 ItemStack;两种来源都拿不到时返回 null
     */
    @Nullable
    public static ItemStack getItemStackByNeuName(String neuName) {
        if (neuName == null || !NEU_NAME_PATTERN.matcher(neuName).matches()) return null;
        ItemStack fromSkyblocker = skyblockerItemStack(neuName);
        if (fromSkyblocker != null) return fromSkyblocker;
        return fallbackRepoItemStack(neuName);
    }

    @Nullable
    private static ItemStack skyblockerItemStack(String neuName) {
        try {
            var stack = ItemRepository.getItemStack(neuName);
            return stack == null ? null : stack.getStack();
        } catch (NoClassDefFoundError e) {
            return null; // Skyblocker 未安装
        }
    }

    /** 本地 item-repo 文件兜底:itemid + displayname + lore 组成基础展示栈 */
    @Nullable
    private static ItemStack fallbackRepoItemStack(String neuName) {
        JsonObject item = repoItemJson(neuName);
        if (item == null) return null;
        String itemId = item.has("itemid") ? item.get("itemid").getAsString() : null;
        if (itemId == null || itemId.isEmpty()) return null;
        // 26.x 命名兼容:旧物品库数据里的 minecraft:skull → minecraft:player_head
        if (itemId.equals("minecraft:skull")) itemId = "minecraft:player_head";
        Identifier mcId = Identifier.tryParse(itemId);
        if (mcId == null) return null;
        var vanillaItem = BuiltInRegistries.ITEM.get(mcId).orElse(null);
        if (vanillaItem == null || vanillaItem.value() == Items.AIR) return null;

        ItemStack stack = new ItemStack(vanillaItem, 1, DataComponentPatch.EMPTY);
        if (item.has("displayname")) {
            // 直接塞 § 文本:MC 渲染时自动解析颜色码,无需手工转样式
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(item.get("displayname").getAsString()));
        }
        if (item.has("lore") && item.get("lore").isJsonArray()) {
            List<Component> lore = new ArrayList<>();
            for (JsonElement line : item.getAsJsonArray("lore")) {
                if (line.isJsonPrimitive()) lore.add(Component.literal(line.getAsString()));
            }
            if (!lore.isEmpty()) stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        // nbttag 里的 ItemModel:让 Hypixel 资源包的自定义模型在图标/悬停渲染时生效
        if (item.has("nbttag")) {
            try {
                JsonObject nbt = JsonParser.parseString(item.get("nbttag").getAsString()).getAsJsonObject();
                if (nbt.has("ItemModel")) {
                    Identifier modelId = Identifier.tryParse(nbt.get("ItemModel").getAsString());
                    if (modelId != null) stack.set(DataComponents.ITEM_MODEL, modelId);
                }
            } catch (Exception ignored) {
            }
        }
        return stack;
    }

    @Nullable
    private static JsonObject repoItemJson(String neuName) {
        JsonObject cached = REPO_ITEM_CACHE.get(neuName);
        if (cached != null) return cached;
        Path file = repoItemsDir().resolve(neuName + ".json");
        if (!Files.isRegularFile(file)) return null;
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            JsonElement je = JsonParser.parseReader(reader);
            if (!je.isJsonObject()) return null;
            JsonObject item = je.getAsJsonObject();
            REPO_ITEM_CACHE.put(neuName, item);
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 取 NEU 物品库文件里的 displayname（legacy 格式，可能带 § 颜色码）。
     * <p>兜底用途：物品栈来自 skyblocker 内存库时不一定带 CUSTOM_NAME 颜色，
     * 用文件里的 displayname 颜色为聊天物品名染色。
     *
     * @return legacy displayname；无文件/无字段返回 null
     */
    @Nullable
    public static String repoDisplayName(String neuName) {
        JsonObject item = repoItemJson(neuName);
        if (item == null || !item.has("displayname")) return null;
        try {
            return item.get("displayname").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 取物品的 legacy 显示名（含 § 颜色码），按优先级：
     * <ol>
     *   <li>{@code DataComponents.CUSTOM_NAME}（1.21+ 新格式）</li>
     *   <li>custom_data 的 {@code display.Name}（Skyblock 旧 NBT 格式，skyblocker 物品库常见）</li>
     *   <li>默认 {@code getDisplayName()} 转 legacy</li>
     * </ol>
     * 用于聊天物品名的颜色兜底：可解析出 § 颜色码时名字据此染色。
     */
    public static String displayNameLegacy(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        var customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            String legacy = ChatUtils.toLegacyString(customName);
            if (legacy != null && !legacy.isEmpty()) return legacy;
        }
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var tag = customData.copyTag();
            // 1.21: display 在 root;老版本在 ExtraAttributes? 实际 skyblocker 物品 custom_data 即旧 display
            var display = tag.getCompound("display").orElse(null);
            if (display != null) {
                String name = display.getString("Name").orElse(null);
                if (name != null && !name.isEmpty()) return name;
            }
            // 兜底: ExtraAttributes.display.Name
            var extra = tag.getCompound("ExtraAttributes").orElse(null);
            if (extra != null) {
                var extraDisplay = extra.getCompound("display").orElse(null);
                if (extraDisplay != null) {
                    String name = extraDisplay.getString("Name").orElse(null);
                    if (name != null && !name.isEmpty()) return name;
                }
            }
        }
        return ChatUtils.toLegacyString(stack.getDisplayName());
    }

    private static Path repoItemsDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("skyblocker").resolve("item-repo").resolve("items");
    }

    /** 药水: potion 字段 + potion_level → POTION_药水;等级 */
    @Nullable
    private static String resolvePotionInternalName(JsonObject tag) {
        String potion = fieldString(tag, "potion");
        Integer level = fieldInt(tag, "potion_level");
        if (potion == null || level == null) return null;
        return "POTION_" + potion.toUpperCase(Locale.ROOT) + ";" + level;
    }

    /** 取 custom_data 的 JSON 形态（根级或 ExtraAttributes 兼容） */
    @Nullable
    private static JsonObject customDataJson(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        JsonElement je = com.mojang.serialization.Dynamic.convert(NbtOps.INSTANCE, JsonOps.INSTANCE, customData.copyTag());
        return je.isJsonObject() ? je.getAsJsonObject() : null;
    }

    /** 宠物: petInfo JSON 串 → type;稀有度序号 */
    @Nullable
    private static String resolvePetInternalName(JsonObject tag) {
        String petInfo = fieldString(tag, "petInfo");
        if (petInfo == null) return null;
        try {
            JsonObject info = JsonParser.parseString(petInfo).getAsJsonObject();
            String type = info.has("type") ? info.get("type").getAsString() : null;
            if (type == null || type.isBlank()) return null;
            String tier = info.has("tier") ? info.get("tier").getAsString() : null;
            Integer index = PET_TIER_INDEX.get(tier);
            return index != null ? type + ";" + index : type;
        } catch (Exception e) {
            return null;
        }
    }

    /** 载体映射合成: 取 map 首个条目 → 前缀 + 键大写 + 分隔 + 等级 */
    @Nullable
    private static String resolveMapInternalName(JsonObject tag, String field, String prefix, String levelSep) {
        JsonObject map = nestedObject(tag, field);
        if (map == null || map.isEmpty()) return null;
        var entry = map.entrySet().iterator().next();
        return prefix + entry.getKey().toUpperCase(Locale.ROOT) + levelSep + entry.getValue().getAsInt();
    }

    /** 根级字段读取，ExtraAttributes 嵌套兼容 */
    @Nullable
    private static String fieldString(JsonObject tag, String field) {
        JsonElement el = fieldElement(tag, field);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
    }

    /** 根级数字字段读取，ExtraAttributes 嵌套兼容 */
    @Nullable
    private static Integer fieldInt(JsonObject tag, String field) {
        JsonElement el = fieldElement(tag, field);
        return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber() ? el.getAsInt() : null;
    }

    @Nullable
    private static JsonElement fieldElement(JsonObject tag, String field) {
        if (tag.has(field)) return tag.get(field);
        JsonObject extra = extraAttributes(tag);
        return extra != null && extra.has(field) ? extra.get(field) : null;
    }

    @Nullable
    private static JsonObject nestedObject(JsonObject tag, String field) {
        if (tag.has(field) && tag.get(field).isJsonObject()) return tag.getAsJsonObject(field);
        JsonObject extra = extraAttributes(tag);
        if (extra != null && extra.has(field) && extra.get(field).isJsonObject()) {
            return extra.getAsJsonObject(field);
        }
        return null;
    }

    @Nullable
    private static JsonObject extraAttributes(JsonObject tag) {
        JsonElement extra = tag.get("ExtraAttributes");
        return extra != null && extra.isJsonObject() ? extra.getAsJsonObject() : null;
    }

    @Nullable
    public static String getItemUuid(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        var tag = customData.copyTag();
        String uuid = tag.getString("uuid").orElse(null);
        if (uuid != null) return uuid;
        var extra = tag.getCompound("ExtraAttributes").orElse(null);
        if (extra == null) return null;
        return extra.getString("uuid").orElse(null);
    }

    /**
     * Formats an ItemStack as a copy-paste string with skyblock id, display name,
     * minecraft id, lore, and custom_data JSON.
     */
    public static String formatItemCopyText(ItemStack item) {
        var sb = new StringBuilder();
        String sbid = getSkyblockId(item);
        if (sbid != null) sb.append("internal name: ").append(sbid).append("\n");
        sb.append("display name: '").append(ChatUtils.toLegacyString(item.getDisplayName())).append("'\n");
        sb.append("minecraft id: '").append(BuiltInRegistries.ITEM.getKey(item.getItem())).append("'\n");
        sb.append("stack: ").append(item.getCount()).append(" / ").append(item.getMaxStackSize()).append("\n");

        var cmd = item.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd != null) sb.append("custom_model_data: ").append(cmd).append("\n");

        var itemModel = item.get(DataComponents.ITEM_MODEL);
        if (itemModel != null) sb.append("item_model: ").append(itemModel).append("\n");

        // 玩家头颅材质
        var skullProfile = getSkullProfile(item);
        if (skullProfile != null) {
            sb.append("skull_owner: '").append(skullProfile.name()).append("'\n");
            sb.append("skull_uuid: ").append(skullProfile.id()).append("\n");
            var texture = getSkullTexture(item);
            if (texture != null) sb.append("skull_texture: ").append(texture).append("\n");
        }

        var loreComp = item.get(DataComponents.LORE);
        if (loreComp != null) {
            sb.append("lore:\n");
            for (var line : loreComp.lines()) {
                sb.append(" '").append(ChatUtils.toLegacyString(line)).append("'\n");
            }
        }

        var customData = item.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            Tag tag = customData.copyTag();
            JsonElement je = com.mojang.serialization.Dynamic.convert(NbtOps.INSTANCE, JsonOps.INSTANCE, tag);
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(je);
            sb.append("\n").append(json);
        }
        return sb.toString();
    }

    /**
     * 获取玩家头颅的 texture 值（base64）。
     *
     * @param stack 物品
     * @return texture value，如果不是玩家头颅或没有 texture 则返回 null
     */
    @Nullable
    public static String getSkullTexture(ItemStack stack) {
        var gp = getSkullProfile(stack);
        if (gp == null) return null;
        var textures = gp.properties().get("textures");
        return textures.stream()
                .filter(Objects::nonNull)
                .map(Property::value)
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private static GameProfile getSkullProfile(ItemStack stack) {
        if (!stack.is(Items.PLAYER_HEAD)) return null;
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null) return null;
        return profile.partialProfile();
    }

    public static boolean isFarmingTool(ItemStack stack) {
        if(stack == null) return false;
        var lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        var lines = lore.lines().iterator();
        return lines.hasNext() && "Farming Tool".equals(ChatUtils.stripColor(lines.next().getString()));
    }

    /**
     * 查询物品单价，优先 Bazaar 卖价 → 最低一口价。
     * <p>SkyBlock 物品只能在 Bazaar 或 AH 中一个渠道交易，AH 上架流程内的物品不会命中
     * Bazaar 分支，此查询对 AH 物品实际返回的就是最低一口价。
     *
     * @param stack 要查询的物品
     * @return 单价，无 Skyblocker / 无 API ID / 无价格数据时返回 -1
     */
    public static double getItemPrice(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -1;

        try {
            // Skyblocker 通过 mixin 把 SkyblockerStack 注入到了 ItemStack 上
            String apiId = ((SkyblockerStack) (Object) stack).getSkyblockApiId();
            if (apiId == null || apiId.isEmpty()) return -1;
            return querySkyblockerPrice(apiId);
        } catch (NoClassDefFoundError e) {
            return -1;
        }
    }

    /**
     * 从 Skyblocker 缓存中查询价格，拆出来是为了让 NoClassDefFoundError
     * 只在真正调用时才触发，不影响 ItemUtils 类本身加载。
     */
    private static double querySkyblockerPrice(String apiId) {
        // 1. Bazaar 卖价
        Object2ObjectMap<String, BazaarProduct> bazaar =
                (Object2ObjectMap<String, BazaarProduct>) TooltipInfoType.BAZAAR.getData();
        if (bazaar != null && bazaar.containsKey(apiId)) {
            OptionalDouble sellPrice = bazaar.get(apiId).sellPrice();
            if (sellPrice.isPresent()) return sellPrice.getAsDouble();
        }

        // 2. 兜底：AH 最低一口价
        Object2DoubleMap<String> lbin =
                (Object2DoubleMap<String>) TooltipInfoType.LOWEST_BINS.getData();
        if (lbin != null && lbin.containsKey(apiId)) {
            return lbin.getDouble(apiId);
        }

        return -1;
    }
}
