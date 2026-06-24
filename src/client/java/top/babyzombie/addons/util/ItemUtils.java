package top.babyzombie.addons.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

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

    public static boolean isFarmingTool(ItemStack stack) {
        if(stack == null) return false;
        var lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        var lines = lore.lines().iterator();
        return lines.hasNext() && "Farming Tool".equals(ChatUtils.stripColor(lines.next().getString()));
    }
}
