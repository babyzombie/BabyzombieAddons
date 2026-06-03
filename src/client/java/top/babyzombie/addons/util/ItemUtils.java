package top.babyzombie.addons.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public final class ItemUtils {
    private ItemUtils() {}

    /**
     * Extracts the SkyBlock item ID from an ItemStack's ExtraAttributes NBT.
     */
    public static String getSkyblockId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        var tag = customData.copyTag();
        var extra = tag.getCompound("ExtraAttributes").orElse(null);
        if (extra == null) return null;
        return extra.getString("id").orElse(null);
    }
}
