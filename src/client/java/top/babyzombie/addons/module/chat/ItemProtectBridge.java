package top.babyzombie.addons.module.chat;

import de.hysky.skyblocker.skyblock.item.ItemProtection;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;

/**
 * 物品保护桥接。优先使用 Skyblocker 的 ItemProtection API，否则回退到自身存储。
 */
public final class ItemProtectBridge {

    private static final boolean SKYBLOCKER = FabricLoader.getInstance().isModLoaded("skyblocker");

    private ItemProtectBridge() {}

    public static boolean needsOwnProtection() {
        return !SKYBLOCKER;
    }

    public static boolean isProtected(ItemStack stack) {
        return ItemProtectStorage.contains(stack);
    }

    public static void toggle(ItemStack stack) {
        if (SKYBLOCKER) {
            ItemProtection.handleKeyPressed(stack);
            return;
        }
        ItemProtectStorage.toggle(stack);
    }
}
