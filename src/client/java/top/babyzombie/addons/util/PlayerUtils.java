package top.babyzombie.addons.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class PlayerUtils {

    private PlayerUtils() {}

    @Nullable
    public static String getCurrentHoldingItemId() {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return null;
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
    }

    public static boolean isHoldingItemContaining(String idPart) {
        String itemId = getCurrentHoldingItemId();
        return itemId != null && itemId.contains(idPart);
    }

    public static int getPing() {
        var player = Minecraft.getInstance().player;
        if (player == null) return -1;
        var entry = player.connection.getPlayerInfo(player.getUUID());
        if (entry == null) return -1;
        return entry.getLatency();
    }

    @Nullable
    public static String getSkyblockProfileId() {
        return HypixelLocationTracker.getInstance().getProfileId();
    }

    @Nullable
    public static String getPlayerUuid() {
        return HypixelLocationTracker.getInstance().getUuid();
    }

    public static boolean isInSkyblock() {
        return HypixelLocationTracker.getInstance().isInSkyblock();
    }

    public static boolean isOnHypixel() {
        return HypixelLocationTracker.getInstance().isOnHypixel();
    }
}
