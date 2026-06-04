package top.babyzombie.addons.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Player state queries: held item, ping, profile, and location checks. */
public final class PlayerUtils {

    private PlayerUtils() {}

    /** @return the registry ID of the currently held item, or null. */
    @Nullable
    public static String getCurrentHoldingItemId() {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return null;
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
    }

    /** Check whether the held item's ID contains the given substring. */
    public static boolean isHoldingItemContaining(String idPart) {
        String itemId = getCurrentHoldingItemId();
        return itemId != null && itemId.contains(idPart);
    }

    /** @return the player's latency in milliseconds, or -1 if unavailable. */
    public static int getPing() {
        var player = Minecraft.getInstance().player;
        if (player == null) return -1;
        var entry = player.connection.getPlayerInfo(player.getUUID());
        if (entry == null) return -1;
        return entry.getLatency();
    }

    /** @return the current SkyBlock profile ID, or null. */
    @Nullable
    public static String getSkyblockProfileId() {
        return HypixelLocationTracker.getInstance().getProfileId();
    }

    /** @return the player's Minecraft UUID, or null. */
    @Nullable
    public static String getPlayerUuid() {
        return HypixelLocationTracker.getInstance().getUuid();
    }

    /** @return true if the player is currently in SkyBlock. */
    public static boolean isInSkyblock() {
        return HypixelLocationTracker.getInstance().isInSkyblock();
    }

    /** @return true if the player is currently on Hypixel. */
    public static boolean isOnHypixel() {
        return HypixelLocationTracker.getInstance().isOnHypixel();
    }
}
