package top.babyzombie.addons.util.pet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Immutable pet data parsed from Hypixel SkyBlock petInfo JSON.
 */
public record PetData(
    String type,
    double exp,
    int tier,
    @Nullable String heldItem,
    int candyUsed,
    @Nullable String uuid,
    @Nullable String uniqueId
) {
    public static final List<String> TIER_NAMES = List.of(
        "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"
    );

    private static final int[][] EXP_TO_NEXT_LEVEL = {
        {100,110,120,130,145,160,175,190,210,230,250,275,300,330,360,400,440,490,540,600,660,730,800,880,960,1050,1150,1260,1380,1510,1650,1800,1960,2130,2310,2500,2700,2920,3160,3420,3700,4000,4350,4750,5200,5700,6300,7000,7800,8700,9700,10800,12000,13300,14700,16200,17800,19500,21300,23200,25200,27400,29800,32400,35200,38200,41400,44800,48400,52200,56200,60400,64800,69400,74200,79200,84700,90700,97200,104200,111700,119700,128200,137200,146700,156700,167700,179700,192700,206700,221700,237700,254700,272700,291700,311700,333700,357700,383700},
        {175,190,210,230,250,275,300,330,360,400,440,490,540,600,660,730,800,880,960,1050,1150,1260,1380,1510,1650,1800,1960,2130,2310,2500,2700,2920,3160,3420,3700,4000,4350,4750,5200,5700,6300,7000,7800,8700,9700,10800,12000,13300,14700,16200,17800,19500,21300,23200,25200,27400,29800,32400,35200,38200,41400,44800,48400,52200,56200,60400,64800,69400,74200,79200,84700,90700,97200,104200,111700,119700,128200,137200,146700,156700,167700,179700,192700,206700,221700,237700,254700,272700,291700,311700,333700,357700,383700,411700,441700,476700,516700,561700,611700},
        {275,300,330,360,400,440,490,540,600,660,730,800,880,960,1050,1150,1260,1380,1510,1650,1800,1960,2130,2310,2500,2700,2920,3160,3420,3700,4000,4350,4750,5200,5700,6300,7000,7800,8700,9700,10800,12000,13300,14700,16200,17800,19500,21300,23200,25200,27400,29800,32400,35200,38200,41400,44800,48400,52200,56200,60400,64800,69400,74200,79200,84700,90700,97200,104200,111700,119700,128200,137200,146700,156700,167700,179700,192700,206700,221700,237700,254700,272700,291700,311700,333700,357700,383700,411700,441700,476700,516700,561700,611700,666700,726700,791700,861700,936700},
        {440,490,540,600,660,730,800,880,960,1050,1150,1260,1380,1510,1650,1800,1960,2130,2310,2500,2700,2920,3160,3420,3700,4000,4350,4750,5200,5700,6300,7000,7800,8700,9700,10800,12000,13300,14700,16200,17800,19500,21300,23200,25200,27400,29800,32400,35200,38200,41400,44800,48400,52200,56200,60400,64800,69400,74200,79200,84700,90700,97200,104200,111700,119700,128200,137200,146700,156700,167700,179700,192700,206700,221700,237700,254700,272700,291700,311700,333700,357700,383700,411700,441700,476700,516700,561700,611700,666700,726700,791700,861700,936700,1016700,1101700,1191700,1286700,1386700},
        {660,730,800,880,960,1050,1150,1260,1380,1510,1650,1800,1960,2130,2310,2500,2700,2920,3160,3420,3700,4000,4350,4750,5200,5700,6300,7000,7800,8700,9700,10800,12000,13300,14700,16200,17800,19500,21300,23200,25200,27400,29800,32400,35200,38200,41400,44800,48400,52200,56200,60400,64800,69400,74200,79200,84700,90700,97200,104200,111700,119700,128200,137200,146700,156700,167700,179700,192700,206700,221700,237700,254700,272700,291700,311700,333700,357700,383700,411700,441700,476700,516700,561700,611700,666700,726700,791700,861700,936700,1016700,1101700,1191700,1286700,1386700,1496700,1616700,1746700,1886700}
    };

    // Pets that can reach level 200
    private static final Set<String> MAX_LEVEL_PETS = Set.of(
        "GOLDEN_DRAGON", "JADE_DRAGON", "ROSE_DRAGON"
    );

    private static final int[] LEGENDARY_TABLE = EXP_TO_NEXT_LEVEL[4];
    private static final long LEVEL_200_XP_REPEAT = LEGENDARY_TABLE[LEGENDARY_TABLE.length - 1]; // 1886700

    /** Calculate the pet's level from its exp, tier, and held item. */
    public int getLevel() {
        double xp = this.exp;
        int level = 1;
        int tierIdx = this.tier;
        if ("PET_ITEM_TIER_BOOST".equals(this.heldItem)) {
            tierIdx++;
        }
        tierIdx = Math.min(tierIdx, 4);

        for (int required : EXP_TO_NEXT_LEVEL[tierIdx]) {
            if (xp < required) break;
            xp -= required;
            level++;
        }

        if (MAX_LEVEL_PETS.contains(this.type) && level >= 100) {
            level++;
            if (xp >= 5555) {
                level++;
                xp -= 5555;
                while (level < 200) {
                    if (xp < LEVEL_200_XP_REPEAT) break;
                    xp -= LEVEL_200_XP_REPEAT;
                    level++;
                }
            }
        }
        return level;
    }

    /** Parse petInfo JSON string from Hypixel NBT. */
    public static PetData fromPetInfo(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        String tierRaw = obj.get("tier").getAsString();
        int tier;
        try {
            tier = Integer.parseInt(tierRaw);
        } catch (NumberFormatException e) {
            tier = TIER_NAMES.indexOf(tierRaw);
            if (tier < 0) tier = 0;
        }
        return new PetData(
            obj.get("type").getAsString(),
            obj.get("exp").getAsDouble(),
            tier,
            obj.has("heldItem") && !obj.get("heldItem").isJsonNull()
                ? obj.get("heldItem").getAsString() : null,
            obj.has("candyUsed") ? obj.get("candyUsed").getAsInt() : 0,
            obj.has("uuid") && !obj.get("uuid").isJsonNull()
                ? obj.get("uuid").getAsString() : null,
            obj.has("uniqueId") && !obj.get("uniqueId").isJsonNull()
                ? obj.get("uniqueId").getAsString() : null
        );
    }

    /** Return a copy of this PetData with the given exp value. */
    public PetData withExp(double newExp) {
        return new PetData(type, newExp, tier, heldItem, candyUsed, uuid, uniqueId);
    }

    /**
     * Look up this pet's primary skill from PetConstants.
     * Returns the SkillType, or null if the pet type is unknown or constants not loaded.
     */
    @Nullable
    public SkillType getPrimarySkill() {
        return PetConstants.getInstance().getPrimarySkill(this.type);
    }

    // ===== Level Progress =====

    /** Detailed XP breakdown at the current level. */
    public record LevelInfo(int level, double xpInLevel, double xpToNext, boolean isMaxed) {}

    /**
     * Returns detailed level information, mirroring {@link #getLevel()} logic
     * but preserving the remaining XP within the current level.
     */
    public LevelInfo getLevelInfo() {
        double xp = this.exp;
        int level = 1;
        int tierIdx = this.tier;
        if ("PET_ITEM_TIER_BOOST".equals(this.heldItem)) {
            tierIdx++;
        }
        tierIdx = Math.min(tierIdx, 4);

        for (int required : EXP_TO_NEXT_LEVEL[tierIdx]) {
            if (xp < required) {
                return new LevelInfo(level, xp, required, false);
            }
            xp -= required;
            level++;
        }

        // Max-level pets (GOLDEN_DRAGON, JADE_DRAGON, ROSE_DRAGON → Lvl 200)
        if (MAX_LEVEL_PETS.contains(this.type) && level >= 100) {
            if (xp < 5555) {
                return new LevelInfo(level + 1, xp, 5555, false);
            }
            level++;
            xp -= 5555;
            while (level < 200) {
                if (xp < LEVEL_200_XP_REPEAT) {
                    return new LevelInfo(level, xp, LEVEL_200_XP_REPEAT, false);
                }
                xp -= LEVEL_200_XP_REPEAT;
                level++;
            }
        }

        return new LevelInfo(level, 0, 0, true);
    }

    // ===== Display Helpers =====

    /** "GOLDEN_DRAGON" → "Golden Dragon" */
    public static String formatPetName(String type) {
        if (type == null) return "?";
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (int i = 0; i < type.length(); i++) {
            char c = type.charAt(i);
            if (c == '_') {
                sb.append(' ');
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
