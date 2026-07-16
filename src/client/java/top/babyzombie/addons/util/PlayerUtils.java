package top.babyzombie.addons.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;

/** Player state queries: held item, ping, profile, skin, and location checks. */
public final class PlayerUtils {

    private PlayerUtils() {}

    /** @return the registry ID of the currently held item, or null. */
    @Nullable
    public static String getCurrentHoldingItemId() {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        var held = player.getMainHandItem();
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

    /**
     * Get the GameProfile for a player entity on the client side.
     * Works for both local and remote players via PlayerInfo or Player#getGameProfile.
     *
     * @param entity the entity to get the profile from
     * @return the player's GameProfile, or null if unavailable
     */
    @Nullable
    public static GameProfile getPlayerProfile(Entity entity) {
        var mc = Minecraft.getInstance();

        // Try PlayerInfo first (works for all tracked players including local)
        var connection = mc.getConnection();
        if (connection != null) {
            var playerInfo = connection.getPlayerInfo(entity.getUUID());
            if (playerInfo != null) {
                return playerInfo.getProfile();
            }
        }

        // Fallback: entity is the local Player instance
        if (entity instanceof Player player) {
            return player.getGameProfile();
        }

        return null;
    }

    /**
     * Extract the skin texture URL from a GameProfile.
     *
     * @param profile the player's GameProfile
     * @return the skin texture URL, or null if not available
     */
    @Nullable
    public static String getSkinTextureUrl(@Nullable GameProfile profile) {
        if (profile == null) return null;
        Collection<Property> textures = profile.properties().get("textures");
        if (textures.isEmpty()) return null;
        String base64 = textures.iterator().next().value();
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            JsonObject obj = JsonParser.parseString(new String(decoded, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonObject texturesObj = obj.getAsJsonObject("textures");
            if (texturesObj == null) return null;
            JsonObject skin = texturesObj.getAsJsonObject("SKIN");
            if (skin == null) return null;
            return skin.get("url").getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}
