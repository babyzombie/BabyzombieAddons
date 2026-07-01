package top.babyzombie.addons.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.util.ChatUtils;

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

    public static boolean isFarmingTool(ItemStack stack) {
        if(stack == null) return false;
        var lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        var lines = lore.lines().iterator();
        return lines.hasNext() && "Farming Tool".equals(ChatUtils.stripColor(lines.next().getString()));
    }
}
