package top.babyzombie.addons.module.misc.pet;

import com.google.common.collect.LinkedHashMultimap;
import com.google.gson.JsonElement;
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
 * Loads pet skin textures from the item repo's animatedskulls.json and items folder.
 *
 * <h3>Two-phase usage:</h3>
 * <ol>
 *   <li><b>Scan time:</b> call {@link #resolveSkinVariant} with the pet's skin family
 *       name, skull texture (base64), and petInfo JSON. Returns the full variant name
 *       which should be stored in {@code PetData.resolvedSkin}.</li>
 *   <li><b>Render time:</b> call {@link #getSkinHead} with the resolved variant name
 *       and current game time for animation frame selection (O(1) map lookup).</li>
 * </ol>
 */
public final class PetSkinTexture {
    private static final Logger LOGGER = LoggerFactory.getLogger("BabyzombieAddons/PetSkinTexture");

    private static final PetSkinTexture INSTANCE = new PetSkinTexture();

    /** SNBT Value extraction pattern (matches Value:&quot;...&quot; in SNBT strings). */
    private static final Pattern SKULL_VALUE_PATTERN =
        Pattern.compile("Value:\"([A-Za-z0-9+/=]+)\"");
    private static final Pattern ITEM_MODEL_PATTERN =
        Pattern.compile("ItemModel:\"([^\"]+)\"");

    // --- Skin animation data ---
    /** Full skin entry key → animation frames. */
    private final Map<String, SkinEntry> skins = new LinkedHashMap<>();
    /** Base skin family → ordered variant name list. */
    private final Map<String, List<String>> skinVariants = new LinkedHashMap<>();
    /** Uppercase base skin family → extraData NBT field name. */
    private final Map<String, String> skinNbtNames = new LinkedHashMap<>();
    /** Texture URL → fully-qualified variant name (for URL-based matching fallback). */
    private final Map<String, String> urlToVariant = new LinkedHashMap<>();

    private boolean loaded;

    private PetSkinTexture() {}

    public static PetSkinTexture getInstance() { return INSTANCE; }

    // ==================================================================
    //  Skin animation entry
    // ==================================================================

    public record SkinEntry(int ticks, List<String> textures) {
        /** Return the frame for the given game time (client ticks). */
        public String frame(long gameTime) {
            if (textures.isEmpty()) return "";
            int idx = (int) ((gameTime / ticks) % textures.size());
            return textures.get(idx);
        }
    }

    // ==================================================================
    //  Loading
    // ==================================================================

    /** Ensure the animatedskulls.json is loaded. Idempotent. */
    public boolean ensureLoaded() {
        if (loaded) return true;

        Path constantsDir = getConstantsDir();
        if (constantsDir == null) {
            LOGGER.warn("[PetSkinTexture] Item repo not found — pet skins unavailable");
            return false;
        }

        Path file = constantsDir.resolve("animatedskulls.json");
        if (!Files.exists(file)) {
            LOGGER.warn("[PetSkinTexture] animatedskulls.json not found");
            return false;
        }

        try {
            String raw = Files.readString(file);
            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
            parseSkins(obj);
            parseVariants(obj);
            parseNbtNames(obj);
            buildUrlIndex();
            loaded = true;
            LOGGER.info("[PetSkinTexture] Loaded {} skins, {} variant families",
                skins.size(), skinVariants.size());
        } catch (IOException e) {
            LOGGER.error("[PetSkinTexture] Failed to load animatedskulls.json", e);
        }
        return loaded;
    }

    // ==================================================================
    //  Phase 1 — Variant resolution (scan time)
    // ==================================================================

    /**
     * Resolve the full skin variant name from the pet's skin family and current texture.
     *
     * @param skin          the {@code skin} field from petInfo JSON (e.g. "GOLDEN_DRAGON_ANCIENT")
     * @param skullTexture  the base64 skull texture from the pet's ItemStack PROFILE
     * @param petInfoJson   the full petInfo JSON string (for extraData extraction)
     * @return the resolved variant name like "PET_SKIN_GOLDEN_DRAGON_ANCIENT_GOLDEN_BABY",
     *         or null if no skin could be resolved
     */
    @Nullable
    public String resolveSkinVariant(@Nullable String skin,
                                     @Nullable String skullTexture,
                                     String petInfoJson) {
        if (skin == null || skin.isEmpty()) return null;
        if (!ensureLoaded()) return null;

        String skinKey = "PET_SKIN_" + skin.toUpperCase(Locale.ROOT);

        // --- Step ①: use pet_skin_variant + pet_skin_nbt_name ---
        List<String> variants = skinVariants.get(skinKey);
        if (variants != null && !variants.isEmpty()) {
            String nbtField = skinNbtNames.get(skinKey);
            if (nbtField != null) {
                int index = getExtraDataIndex(petInfoJson, nbtField);
                if (index >= 0 && index < variants.size()) {
                    return variants.get(index);
                }
                // Index out of range or not found — fall back to URL match
            }
            // No NBT field — try URL match
            return matchByUrl(variants, skullTexture);
        }

        // --- Step ②: scan skins for matching prefix ---
        String prefix = skinKey + "_";
        List<String> matching = new ArrayList<>();
        for (String key : skins.keySet()) {
            if (key.startsWith(prefix)) {
                matching.add(key);
            }
        }
        if (!matching.isEmpty()) {
            Collections.sort(matching);
            return matchByUrl(matching, skullTexture);
        }

        // --- Step ③: check items/ folder for static skin ---
        if (skinItemExists(skinKey)) {
            return skinKey;
        }

        return null;
    }

    // ==================================================================
    //  Phase 2 — Texture retrieval (render time, O(1))
    // ==================================================================

    /**
     * Get an ItemStack for the given skin variant, selecting the appropriate
     * animation frame based on game time.
     *
     * @param resolvedSkin the full variant name from {@link #resolveSkinVariant}
     * @param gameTime     current client world tick (for animation frame selection)
     * @return the textured ItemStack, or null if the variant is not found
     */
    @Nullable
    public ItemStack getSkinHead(String resolvedSkin, long gameTime) {
        if (resolvedSkin == null || !ensureLoaded()) return null;

        // Try animated skins first
        SkinEntry entry = skins.get(resolvedSkin);
        if (entry != null) {
            String base64 = entry.frame(gameTime);
            return createSkullStack(base64);
        }

        // Fall back to items/ folder
        return loadFromItemsFile(resolvedSkin);
    }

    // ==================================================================
    //  Internal: parsing
    // ==================================================================

    private void parseSkins(JsonObject root) {
        JsonObject skinsObj = root.getAsJsonObject("skins");
        if (skinsObj == null) return;
        for (Map.Entry<String, JsonElement> e : skinsObj.entrySet()) {
            JsonObject entry = e.getValue().getAsJsonObject();
            int ticks = entry.has("ticks") ? entry.get("ticks").getAsInt() : 1;
            List<String> textures = new ArrayList<>();
            for (JsonElement tex : entry.getAsJsonArray("textures")) {
                textures.add(tex.getAsString());
            }
            skins.put(e.getKey(), new SkinEntry(ticks, textures));
        }
    }

    private void parseVariants(JsonObject root) {
        JsonObject vObj = root.getAsJsonObject("pet_skin_variant");
        if (vObj == null) return;
        for (Map.Entry<String, JsonElement> e : vObj.entrySet()) {
            List<String> list = new ArrayList<>();
            for (JsonElement el : e.getValue().getAsJsonArray()) {
                list.add(el.getAsString());
            }
            skinVariants.put(e.getKey(), list);
        }
    }

    private void parseNbtNames(JsonObject root) {
        var arr = root.getAsJsonArray("pet_skin_nbt_name");
        if (arr == null || arr.isEmpty()) return;
        List<String> nbtNames = new ArrayList<>();
        for (JsonElement el : arr) {
            nbtNames.add(el.getAsString());
        }
        // Match nbt_names to variant families by token similarity,
        // NOT by order (they are in different orders).
        for (String variantFamily : skinVariants.keySet()) {
            String shortKey = variantFamily.replace("PET_SKIN_", "").toLowerCase(Locale.ROOT);
            String bestMatch = findBestNbtMatch(shortKey, nbtNames);
            if (bestMatch != null) {
                skinNbtNames.put(variantFamily, bestMatch);
            }
        }
    }

    /** Find the nbt_name that best matches the variant family short key. */
    @Nullable
    private static String findBestNbtMatch(String shortKey, List<String> nbtNames) {
        String best = null;
        int bestScore = 0;
        // Tokenize the short key for matching
        String[] tokens = shortKey.split("_");
        for (String nbtName : nbtNames) {
            int score = 0;
            String nbtLower = nbtName.toLowerCase(Locale.ROOT);
            for (String token : tokens) {
                if (nbtLower.contains(token)) score++;
            }
            // Special case: "gdrag" / "dragon" distinction
            if (shortKey.contains("golden_dragon") && nbtLower.contains("gdrag")) score++;
            if (shortKey.contains("ender_dragon") && nbtLower.contains("dragon") && !nbtLower.contains("gdrag")) score++;
            if (score > bestScore) {
                bestScore = score;
                best = nbtName;
            }
        }
        return bestScore > 0 ? best : null;
    }

    /** Build URL → variant name index for fast matching. */
    private void buildUrlIndex() {
        for (var entry : skins.entrySet()) {
            String variantName = entry.getKey();
            for (String tex : entry.getValue().textures()) {
                String url = extractUrl(tex);
                if (url != null) {
                    urlToVariant.put(url, variantName);
                }
            }
        }
    }

    // ==================================================================
    //  Internal: helpers
    // ==================================================================

    /** Match the current skull texture URL against a list of variant names. */
    @Nullable
    private String matchByUrl(List<String> variantNames, @Nullable String skullTexture) {
        if (skullTexture == null || skullTexture.isEmpty()) {
            // If there's only one variant, assume it's that one
            return variantNames.size() == 1 ? variantNames.get(0) : null;
        }
        String currentUrl = extractUrl(skullTexture);
        if (currentUrl == null) return null;

        // Direct lookup in urlToVariant (covers all skins, not just the variant list)
        String match = urlToVariant.get(currentUrl);
        if (match != null && variantNames.contains(match)) {
            return match;
        }

        // Fallback: iterate the variant list and check each one's textures
        for (String variantName : variantNames) {
            SkinEntry entry = skins.get(variantName);
            if (entry == null) continue;
            for (String tex : entry.textures()) {
                if (currentUrl.equals(extractUrl(tex))) {
                    return variantName;
                }
            }
        }
        return null;
    }

    /** Extract the texture URL from a base64 skin value (may have "UUID:" prefix). */
    @Nullable
    private static String extractUrl(String base64) {
        try {
            // Strip "UUID:" prefix if present (animatedskulls.json format)
            int colonIdx = base64.indexOf(':');
            String pureBase64 = colonIdx > 0 ? base64.substring(colonIdx + 1) : base64;
            byte[] decoded = Base64.getDecoder().decode(pureBase64);
            JsonObject obj = JsonParser.parseString(new String(decoded, StandardCharsets.UTF_8))
                .getAsJsonObject();
            JsonObject textures = obj.getAsJsonObject("textures");
            if (textures == null) return null;
            JsonObject skin = textures.getAsJsonObject("SKIN");
            if (skin == null) return null;
            JsonElement url = skin.get("url");
            return url != null ? url.getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Extract an extraData field index from petInfo JSON. */
    private static int getExtraDataIndex(String petInfoJson, String nbtField) {
        try {
            JsonObject obj = JsonParser.parseString(petInfoJson).getAsJsonObject();
            JsonObject extraData = obj.getAsJsonObject("extraData");
            if (extraData == null) return -1;
            JsonElement val = extraData.get(nbtField);
            if (val == null) return -1;
            return (int) val.getAsDouble();
        } catch (Exception e) {
            return -1;
        }
    }

    /** Check whether a pet skin item file exists in the items/ folder. */
    private boolean skinItemExists(String variantName) {
        Path itemsDir = getItemsDir();
        if (itemsDir == null) return false;
        return Files.exists(itemsDir.resolve(variantName + ".json"));
    }

    // ==================================================================
    //  Internal: ItemStack creation
    // ==================================================================

    /** Create a player-head ItemStack from a base64 skin texture value. */
    @Nullable
    public static ItemStack createSkullStack(String base64) {
        if (base64 == null || base64.isEmpty()) return null;

        // Animatedskulls.json textures are "UUID:base64" — strip the UUID prefix
        int colonIdx = base64.indexOf(':');
        String pureBase64 = colonIdx > 0 ? base64.substring(colonIdx + 1) : base64;

        var stack = new ItemStack(Items.PLAYER_HEAD);
        UUID uuid = UUID.nameUUIDFromBytes(pureBase64.getBytes(StandardCharsets.UTF_8));
        var multimap = LinkedHashMultimap.<String, Property>create();
        multimap.put("textures", new Property("textures", pureBase64, null));
        var gp = new GameProfile(uuid, "", new PropertyMap(multimap));
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(gp));
        return stack;
    }

    /** Load a single-frame pet skin from the items/ folder. */
    @Nullable
    private static ItemStack loadFromItemsFile(String variantName) {
        Path itemsDir = getItemsDir();
        if (itemsDir == null) return null;

        Path file = itemsDir.resolve(variantName + ".json");
        if (!Files.exists(file)) return null;

        try {
            String raw = Files.readString(file);
            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();

            String itemId = obj.has("itemid") ? obj.get("itemid").getAsString() : null;
            if (itemId == null) return null;
            int damage = obj.has("damage") ? obj.get("damage").getAsInt() : 0;
            Item item = resolveSkullItem(itemId, damage);
            if (item == null) return null;

            var stack = new ItemStack(item);
            String nbttag = obj.has("nbttag") ? obj.get("nbttag").getAsString() : null;
            if (nbttag != null) {
                Matcher skullM = SKULL_VALUE_PATTERN.matcher(nbttag);
                if (skullM.find()) {
                    stack.set(DataComponents.PROFILE, createProfile(skullM.group(1)));
                }
                Matcher modelM = ITEM_MODEL_PATTERN.matcher(nbttag);
                if (modelM.find()) {
                    stack.set(DataComponents.ITEM_MODEL, Identifier.parse(modelM.group(1)));
                }
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

    /** Map legacy minecraft:skull + damage to modern item types. */
    @Nullable
    private static Item resolveSkullItem(String itemId, int damage) {
        if ("minecraft:skull".equals(itemId)) {
            return switch (damage) {
                case 0 -> Items.SKELETON_SKULL;
                case 1 -> Items.WITHER_SKELETON_SKULL;
                case 2 -> Items.ZOMBIE_HEAD;
                case 4 -> Items.CREEPER_HEAD;
                case 5 -> Items.DRAGON_HEAD;
                default -> Items.PLAYER_HEAD;
            };
        }
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
    }

    private static ResolvableProfile createProfile(String base64Value) {
        UUID uuid = UUID.nameUUIDFromBytes(base64Value.getBytes(StandardCharsets.UTF_8));
        var multimap = LinkedHashMultimap.<String, Property>create();
        multimap.put("textures", new Property("textures", base64Value, null));
        var gp = new GameProfile(uuid, "", new PropertyMap(multimap));
        return ResolvableProfile.createResolved(gp);
    }

    // ==================================================================
    //  Path helpers
    // ==================================================================

    @Nullable
    private static Path getConstantsDir() {
        PetConstants constants = PetConstants.getInstance();
        constants.ensureLoaded();
        Path root = constants.getRepoRoot();
        if (root == null) return null;
        return root.resolve("constants");
    }

    @Nullable
    private static Path getItemsDir() {
        PetConstants constants = PetConstants.getInstance();
        constants.ensureLoaded();
        return constants.getItemsDir();
    }
}
