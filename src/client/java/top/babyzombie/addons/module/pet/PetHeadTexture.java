package top.babyzombie.addons.module.pet;

import com.google.common.collect.LinkedHashMultimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.babyzombie.addons.util.pet.PetConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads pet head textures from the item repo and caches ItemStacks for HUD rendering.
 *
 * Pet heads are stored as {@code items/{TYPE};{tier}.json} in the repo.
 * We search tier 5 (MYTHIC) down to 0 (COMMON) to find the best available texture.
 *
 * Held item icons are stored as {@code items/{ITEM_ID}.json} (no tier suffix).
 */
public final class PetHeadTexture {
    private static final Logger LOGGER = LoggerFactory.getLogger("BabyzombieAddons/PetHeadTexture");

    /** Extracts the base64 skull texture Value from an SNBT string. */
    private static final Pattern SKULL_VALUE_PATTERN =
        Pattern.compile("Value:\"([A-Za-z0-9+/=]+)\"");

    private static final Map<String, ItemStack> cache = new HashMap<>();
    private static boolean fallbackWarned;

    private PetHeadTexture() {}

    /**
     * Get a player-head ItemStack for a pet type at its best available tier.
     * Looks for {@code items/{TYPE};{tier}.json} from tier 5 down to 0.
     */
    public static ItemStack getPetHead(String petType) {
        String key = petType.toUpperCase(Locale.ROOT);
        ItemStack cached = cache.get(key);
        if (cached != null) return cached.copy();

        ItemStack head = loadFromRepo(petType, true);
        if (head != null) {
            cache.put(key, head);
            return head.copy();
        }

        warnFallback();
        ItemStack fallback = new ItemStack(Items.PLAYER_HEAD);
        cache.put(key, fallback);
        return fallback.copy();
    }

    /**
     * Get an ItemStack icon for a held item or any repo item by its ID.
     * Looks for {@code items/{itemId}.json} (no tier suffix).
     */
    public static ItemStack getItemIcon(String itemId) {
        String key = "item:" + itemId.toUpperCase(Locale.ROOT);
        ItemStack cached = cache.get(key);
        if (cached != null) return cached.copy();

        ItemStack icon = loadFromRepo(itemId, false);
        if (icon != null) {
            cache.put(key, icon);
            return icon.copy();
        }

        warnFallback();
        ItemStack fallback = new ItemStack(Items.PLAYER_HEAD);
        cache.put(key, fallback);
        return fallback.copy();
    }

    /**
     * Get the coloured display name for a held item from the item repo.
     * Falls back to a plain-formatted name if the repo isn't available.
     */
    public static String getItemDisplayName(String itemId) {
        Path itemsDir = getItemsDir();
        if (itemsDir != null) {
            Path file = itemsDir.resolve(itemId + ".json");
            if (Files.exists(file)) {
                try {
                    String raw = Files.readString(file);
                    JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
                    return obj.get("displayname").getAsString();
                } catch (IOException | RuntimeException ignored) {}
            }
        }
        // Fallback: format from internal ID
        return "§f" + formatName(itemId);
    }

    /** Clear the head/texture cache (e.g. on profile switch). */
    public static void clearCache() {
        cache.clear();
        fallbackWarned = false;
    }

    // ===== Internal =====

    @Nullable
    private static ItemStack loadFromRepo(String id, boolean isPet) {
        Path itemsDir = getItemsDir();
        if (itemsDir == null) return null;

        if (isPet) {
            // Try highest tier first (5 = MYTHIC → 0 = COMMON)
            for (int tier = 5; tier >= 0; tier--) {
                Path file = itemsDir.resolve(id + ";" + tier + ".json");
                ItemStack stack = tryLoadFile(file);
                if (stack != null) return stack;
            }
        } else {
            Path file = itemsDir.resolve(id + ".json");
            return tryLoadFile(file);
        }
        return null;
    }

    @Nullable
    private static ItemStack tryLoadFile(Path file) {
        if (!Files.exists(file)) return null;
        try {
            String raw = Files.readString(file);
            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
            String nbttag = obj.get("nbttag").getAsString();

            Matcher m = SKULL_VALUE_PATTERN.matcher(nbttag);
            if (!m.find()) return null;

            String base64Value = m.group(1);
            return createSkull(base64Value);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static ItemStack createSkull(String base64Value) {
        // Derive a deterministic UUID from the texture content
        UUID uuid = UUID.nameUUIDFromBytes(base64Value.getBytes(StandardCharsets.UTF_8));
        var multimap = LinkedHashMultimap.<String, Property>create();
        multimap.put("textures", new Property("textures", base64Value, null));
        var gp = new GameProfile(uuid, "", new PropertyMap(multimap));

        var stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(gp));
        return stack;
    }

    @Nullable
    private static Path getItemsDir() {
        PetConstants constants = PetConstants.getInstance();
        constants.ensureLoaded();
        return constants.getItemsDir();
    }

    /** "PET_ITEM_MINOS_RELIC" → "Minos Relic" (fallback when repo unavailable) */
    private static String formatName(String id) {
        String name = id.replace("PET_ITEM_", "").replace("_", " ").toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == ' ') { sb.append(' '); nextUpper = true; }
            else if (nextUpper) { sb.append(Character.toUpperCase(c)); nextUpper = false; }
            else { sb.append(c); }
        }
        return sb.toString();
    }

    private static void warnFallback() {
        if (!fallbackWarned) {
            fallbackWarned = true;
            LOGGER.warn("[PetHeadTexture] Item repo not available — using fallback head");
        }
    }
}
