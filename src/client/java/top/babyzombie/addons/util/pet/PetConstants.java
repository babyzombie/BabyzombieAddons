package top.babyzombie.addons.util.pet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads pet-related and skill leveling constants from the item repo.
 * Tries three paths: skyblocker → firmament → NEU.
 */
public final class PetConstants {
    private static final Logger LOGGER = LoggerFactory.getLogger("BabyzombieAddons/PetConstants");

    private static PetConstants INSTANCE;
    private boolean loaded;

    /** PET_TYPE → primary skill */
    private final Map<String, SkillType> petTypes = new HashMap<>();
    /** custom_pet_leveling entries from pets.json */
    private JsonObject customPetLeveling;
    /** Standard pet level exp thresholds (LEGENDARY table) */
    private int[] petLevels;
    /** pet_rarity_offset map */
    private Map<String, Integer> petRarityOffset;
    /** Path to the resolved item repo root, or null */
    @Nullable
    private Path repoRoot;

    // ===== Skill leveling XP tables from leveling.json =====
    /** Standard skill XP per level (index 0 = lvl 1). Cumulative = sum up to level. */
    private int[] levelingXp;
    /** Different curve for Runecrafting (25 levels) */
    private int[] runecraftingXp;
    /** Different curve for Social (25 levels) */
    private int[] socialXp;
    /** Dungeoneering / Catacombs XP curve */
    private int[] catacombsXp;
    /** Max level per skill */
    private Map<String, Integer> levelingCaps;
    /** Pre-computed cumulative XP per level for standard skills */
    private long[] cumulativeXp;

    private PetConstants() {}

    public static PetConstants getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PetConstants();
        }
        return INSTANCE;
    }

    public boolean ensureLoaded() {
        if (loaded) return true;
        Path root = resolveItemRepo();
        if (root == null) {
            LOGGER.warn("[PetConstants] No item repo found (skyblocker/firmament/NEU)");
            return false;
        }
        repoRoot = root;
        Path constantsDir = root.resolve("constants");

        // Load pets.json
        Path petsJson = constantsDir.resolve("pets.json");
        if (Files.exists(petsJson)) {
            try {
                String raw = Files.readString(petsJson);
                JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
                parsePets(obj);
            } catch (IOException e) {
                LOGGER.error("[PetConstants] Failed to read pets.json", e);
            }
        }

        // Load leveling.json
        Path levelingJson = constantsDir.resolve("leveling.json");
        if (Files.exists(levelingJson)) {
            try {
                String raw = Files.readString(levelingJson);
                JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
                parseLeveling(obj);
            } catch (IOException e) {
                LOGGER.error("[PetConstants] Failed to read leveling.json", e);
            }
        }

        loaded = true;
        return true;
    }

    // ===== Skill Level Getters =====

    @Nullable
    public int[] getLevelingXp() { return levelingXp; }

    @Nullable
    public int[] getRunecraftingXp() { return runecraftingXp; }

    @Nullable
    public int[] getSocialXp() { return socialXp; }

    @Nullable
    public int[] getCatacombsXp() { return catacombsXp; }

    /** Get the total cumulative XP required to reach the given level (1-based).
     *  Returns -1 if level is out of range. */
    public long getCumulativeXpForLevel(int level) {
        if (cumulativeXp == null || level < 1 || level > cumulativeXp.length) return -1;
        return cumulativeXp[level - 1];
    }

    /** Get cumulative XP for a skill type and level. Handles non-standard curves. */
    public long getCumulativeXp(SkillType skill, int level) {
        if (level < 1) return 0;
        int[] table = getTableForSkill(skill);
        if (table == null) table = levelingXp;
        if (table == null || level > table.length) return -1;

        long total = 0;
        for (int i = 0; i < level; i++) {
            total += table[i];
        }
        return total;
    }

    private int[] getTableForSkill(SkillType skill) {
        return switch (skill) {
            case RUNECRAFTING -> runecraftingXp;
            case SOCIAL -> socialXp;
            case DUNGEONEERING -> catacombsXp;
            default -> levelingXp;
        };
    }

    // ===== Pet Getters (unchanged) =====

    @Nullable
    public SkillType getPrimarySkill(String petType) {
        return petTypes.get(petType);
    }

    public double getXpMultiplier(String petType) {
        if (customPetLeveling == null) return 1.0;
        JsonObject entry = customPetLeveling.getAsJsonObject(petType);
        if (entry == null) return 1.0;
        JsonElement mult = entry.get("xp_multiplier");
        if (mult == null) return 1.0;
        return mult.getAsDouble();
    }

    public int[] getPetLevels() { return petLevels; }
    public Map<String, Integer> getPetRarityOffset() { return petRarityOffset; }

    public int getRarityOffset(String tierName) {
        if (petRarityOffset == null) return 0;
        return petRarityOffset.getOrDefault(tierName, 0);
    }

    public boolean hasCustomLeveling(String petType) {
        if (customPetLeveling == null) return false;
        return customPetLeveling.has(petType);
    }

    @Nullable
    public JsonObject getCustomLeveling(String petType) {
        if (customPetLeveling == null) return null;
        return customPetLeveling.getAsJsonObject(petType);
    }

    @Nullable
    public Path getRepoRoot() { return repoRoot; }

    @Nullable
    public Path getItemsDir() {
        if (repoRoot == null) return null;
        return repoRoot.resolve("items");
    }

    // ===== Parsing =====

    private void parsePets(JsonObject obj) {
        JsonObject types = obj.getAsJsonObject("pet_types");
        if (types != null) {
            for (Map.Entry<String, JsonElement> e : types.entrySet()) {
                String skillName = e.getValue().getAsString();
                SkillType skill;
                try {
                    skill = SkillType.valueOf(skillName);
                } catch (IllegalArgumentException ex) {
                    skill = null;
                }
                petTypes.put(e.getKey(), skill);
            }
        }

        customPetLeveling = obj.getAsJsonObject("custom_pet_leveling");

        petLevels = new int[0];
        JsonElement levels = obj.get("pet_levels");
        if (levels != null && levels.isJsonArray()) {
            var arr = levels.getAsJsonArray();
            petLevels = new int[arr.size()];
            for (int i = 0; i < arr.size(); i++) petLevels[i] = arr.get(i).getAsInt();
        }

        petRarityOffset = new LinkedHashMap<>();
        JsonObject offset = obj.getAsJsonObject("pet_rarity_offset");
        if (offset != null) {
            for (Map.Entry<String, JsonElement> e : offset.entrySet())
                petRarityOffset.put(e.getKey(), e.getValue().getAsInt());
        }
    }

    private void parseLeveling(JsonObject obj) {
        // Standard XP per level
        JsonElement xpElem = obj.get("leveling_xp");
        if (xpElem != null && xpElem.isJsonArray()) {
            var arr = xpElem.getAsJsonArray();
            levelingXp = new int[arr.size()];
            for (int i = 0; i < arr.size(); i++) levelingXp[i] = arr.get(i).getAsInt();
            // Pre-compute cumulative
            cumulativeXp = new long[levelingXp.length];
            long sum = 0;
            for (int i = 0; i < levelingXp.length; i++) {
                sum += levelingXp[i];
                cumulativeXp[i] = sum;
            }
        }

        // Runecrafting curve
        JsonElement rcElem = obj.get("runecrafting_xp");
        if (rcElem != null && rcElem.isJsonArray()) {
            var arr = rcElem.getAsJsonArray();
            runecraftingXp = new int[arr.size()];
            for (int i = 0; i < arr.size(); i++) runecraftingXp[i] = arr.get(i).getAsInt();
        }

        // Social curve
        JsonElement socElem = obj.get("social");
        if (socElem != null && socElem.isJsonArray()) {
            var arr = socElem.getAsJsonArray();
            socialXp = new int[arr.size()];
            for (int i = 0; i < arr.size(); i++) socialXp[i] = arr.get(i).getAsInt();
        }

        // Dungeoneering (catacombs) curve
        JsonElement cataElem = obj.get("catacombs");
        if (cataElem != null && cataElem.isJsonArray()) {
            var arr = cataElem.getAsJsonArray();
            catacombsXp = new int[arr.size()];
            for (int i = 0; i < arr.size(); i++) catacombsXp[i] = arr.get(i).getAsInt();
        }

        // Level caps
        JsonObject caps = obj.getAsJsonObject("leveling_caps");
        if (caps != null) {
            levelingCaps = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : caps.entrySet())
                levelingCaps.put(e.getKey(), e.getValue().getAsInt());
        }
    }

    @Nullable
    private static Path resolveItemRepo() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path p = gameDir.resolve("config").resolve("skyblocker").resolve("item-repo");
        if (Files.isDirectory(p)) return p;
        p = gameDir.resolve(".firmament").resolve("repo-extracted");
        if (Files.isDirectory(p)) return p;
        p = gameDir.resolve("config").resolve("notenoughupdates").resolve("repo");
        if (Files.isDirectory(p)) return p;
        return null;
    }
}
