package top.babyzombie.addons.module.misc.pet;

import com.google.common.collect.LinkedHashMultimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
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
    /** Extracts ItemModel from an SNBT string, e.g. {@code ItemModel:"minecraft:iron_pickaxe"}. */
    private static final Pattern ITEM_MODEL_PATTERN =
        Pattern.compile("ItemModel:\"([^\"]+)\"");

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
     * Get a player-head ItemStack for a pet, optionally with its equipped skin.
     *
     * @param petType      the pet type (e.g. "GOLDEN_DRAGON")
     * @param resolvedSkin the resolved skin variant name, or null
     * @param gameTime     current client world tick for animation frame selection
     */
    public static ItemStack getPetHead(String petType, @Nullable String resolvedSkin, long gameTime) {
        if (resolvedSkin != null) {
            var config = top.babyzombie.addons.config.ModConfigManager.get().skyblock;
            if (config.pet.showPetSkin) {
                ItemStack skinHead = PetSkinTexture.getInstance().getSkinHead(resolvedSkin, gameTime);
                if (skinHead != null) return skinHead;
            }
        }
        return getPetHead(petType);
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

            // Resolve base item (handle legacy "minecraft:skull" + damage=3 → PLAYER_HEAD)
            String itemId = obj.has("itemid") ? obj.get("itemid").getAsString() : null;
            if (itemId == null) return null;
            int damage = obj.has("damage") ? obj.get("damage").getAsInt() : 0;
            Item item;
            if ("minecraft:skull".equals(itemId)) {
                item = legacySkullItem(damage);
            } else {
                item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
            }
            if (item == null) return null;
            var stack = new ItemStack(item);

            // Apply NBT data components
            String nbttag = obj.has("nbttag") ? obj.get("nbttag").getAsString() : null;
            if (nbttag != null) {
                // Skull texture → PROFILE (regex for old SNBT compatibility)
                Matcher skullM = SKULL_VALUE_PATTERN.matcher(nbttag);
                if (skullM.find()) {
                    stack.set(DataComponents.PROFILE, createSkullProfile(skullM.group(1)));
                }
                // ItemModel → ITEM_MODEL component
                Matcher modelM = ITEM_MODEL_PATTERN.matcher(nbttag);
                if (modelM.find()) {
                    stack.set(DataComponents.ITEM_MODEL, Identifier.parse(modelM.group(1)));
                }
                // Complete NBT → CUSTOM_DATA
                try {
                    CompoundTag tag = TagParser.parseCompoundFully(nbttag);
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                } catch (Exception ignored) {}
            }
            return stack;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Map legacy "minecraft:skull" damage values to modern skull items. */
    private static Item legacySkullItem(int damage) {
        return switch (damage) {
            case 0 -> Items.SKELETON_SKULL;
            case 1 -> Items.WITHER_SKELETON_SKULL;
            case 2 -> Items.ZOMBIE_HEAD;
            case 4 -> Items.CREEPER_HEAD;
            case 5 -> Items.DRAGON_HEAD;
            default -> Items.PLAYER_HEAD; // damage=3 or any other
        };
    }

    /** Create a ResolvableProfile from a base64 skull texture value. */
    private static ResolvableProfile createSkullProfile(String base64Value) {
        UUID uuid = UUID.nameUUIDFromBytes(base64Value.getBytes(StandardCharsets.UTF_8));
        var multimap = LinkedHashMultimap.<String, Property>create();
        multimap.put("textures", new Property("textures", base64Value, null));
        var gp = new GameProfile(uuid, "", new PropertyMap(multimap));
        return ResolvableProfile.createResolved(gp);
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
