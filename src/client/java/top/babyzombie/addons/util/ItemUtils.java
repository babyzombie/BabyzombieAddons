package top.babyzombie.addons.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.serialization.JsonOps;
import de.hysky.skyblocker.injected.SkyblockerStack;
import de.hysky.skyblocker.skyblock.item.tooltip.info.TooltipInfoType;
import de.hysky.skyblocker.utils.BazaarProduct;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.OptionalDouble;

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
