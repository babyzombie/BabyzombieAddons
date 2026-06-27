package top.babyzombie.addons.util.pet;

import top.babyzombie.addons.util.pet.state.PlayerPetState;

/**
 * Implements the Hypixel SkyBlock pet experience formula.
 * Stateless — all parameters are passed in.
 *
 * Base formula:
 *   SummonedPetXP = SkillXP × MatchMultiplier × Taming(1+Lvl%) × PetItemMult
 *                   × Diana(1.35) × Reindeer(2.0) × BeastmasterCrest × BattleXP
 *
 * Shared formula:
 *   SharedPetXP = SummonedPetXP × Share% × SharedMatchMult × SharedPetItemMult
 */
public final class PetXPCalculator {
    private PetXPCalculator() {}

    // ===== Core Calculation =====

    /**
     * Calculate XP gained by the summoned pet from a skill XP drop.
     *
     * @param skillXP        raw skill XP gained
     * @param skill          the skill type that generated the XP
     * @param petType        the summoned pet's type (e.g. "RABBIT")
     * @param tamingLevel    player's Taming skill level
     * @param petItemMult    multiplier from the pet's held item (1.0 = no bonus)
     * @param beastmasterMult multiplier from Beastmaster Crest (range 1.0–1.05)
     * @param battleXpLevel  Battle Experience attribute level (0 = none, max 10)
     * @param dianaPetXpBuff whether Diana's "Pet XP Buff" perk is active (×1.35)
     * @return final pet XP for the summoned pet
     */
    public static double calcSummonedPetXP(
        double skillXP,
        SkillType skill,
        String petType,
        int tamingLevel,
        double petItemMult,
        double beastmasterMult,
        int battleXpLevel,
        boolean dianaPetXpBuff
    ) {
        double match = getMatchMultiplier(petType, skill);
        double taming = getTamingMultiplier(tamingLevel);

        // Special: Reindeer gets ×2 from constants
        PetConstants constants = PetConstants.getInstance();
        constants.ensureLoaded();
        double reindeerMult = constants.getXpMultiplier(petType); // 2.0 for REINDEER, 1.0 otherwise

        // Battle Experience: 1 + level*0.01, max 1.10 — only affects Combat
        double battleMult = 1.0;
        if (skill == SkillType.COMBAT) {
            battleMult = 1.0 + battleXpLevel * 0.01;
            if (battleMult > 1.10) battleMult = 1.10;
        }

        double dianaMult = dianaPetXpBuff ? 1.35 : 1.0;

        // Mining/Fishing skills give ×1.5 bonus for all pets
        double skillBonus = (skill == SkillType.MINING || skill == SkillType.FISHING) ? 1.5 : 1.0;

        return skillXP
            * match
            * taming
            * petItemMult
            * beastmasterMult
            * battleMult
            * reindeerMult
            * dianaMult
            * skillBonus;
    }

    /**
     * Calculate XP gained by a pet in an exp share slot.
     *
     * The summoned pet XP = skillXP × summonedMatch × summonedItem × allPlayerMultipliers.
     * The shared pet XP = skillXP × sharePct × sharedMatch × sharedItem × allPlayerMultipliers.
     *
     * So from summonedXP we divide out the summoned pet's specific multipliers
     * and multiply in the shared pet's:
     *   sharedXP = summonedXP × sharePct × (sharedMatch / summonedMatch) × (sharedItem / summonedItem)
     *
     * @param summonedXP            XP gained by the summoned pet (from calcSummonedPetXP)
     * @param summonedPetType       type of the summoned pet (to divide out its match)
     * @param summonedPetItemMult   summoned pet's held-item multiplier (to divide out)
     * @param sharedPetType         type of the pet in the share slot
     * @param sharedPetItemMult     shared pet's held-item multiplier
     * @param skill                 the skill type that generated the XP
     * @param tamingLevel           player's Taming skill level (for share percentage)
     * @param hasExpShareItem       whether this shared pet has an Exp Share item equipped
     * @param dianaSharingIsCaring  whether Diana's "Sharing is Caring" perk is active (+10%)
     * @param whyNotMoreLevel       Why Not More attribute level (+1% exp share per level)
     * @return final pet XP for this shared pet
     */
    public static double calcSharedPetXP(
        double summonedXP,
        String summonedPetType,
        double summonedPetItemMult,
        String sharedPetType,
        double sharedPetItemMult,
        SkillType skill,
        int tamingLevel,
        boolean hasExpShareItem,
        boolean dianaSharingIsCaring,
        int whyNotMoreLevel
    ) {
        // Share percentage: Taming base (0.2%/lvl) + Exp Share item (15%) + Diana (10%) + Why Not More (1%/lvl)
        double sharePct = tamingLevel * 0.2;
        if (hasExpShareItem) sharePct += 15.0;
        if (dianaSharingIsCaring) sharePct += 10.0;
        sharePct += whyNotMoreLevel;
        sharePct = Math.min(sharePct / 100.0, 1.0);

        // Cancel out summoned pet's match & item, apply shared pet's
        double summonedMatch = getMatchMultiplier(summonedPetType, skill);
        double sharedMatch = getMatchMultiplier(sharedPetType, skill);

        return summonedXP * sharePct * (sharedMatch / summonedMatch) * (sharedPetItemMult / summonedPetItemMult);
    }

    // ===== Multiplier Helpers =====

    /**
     * Get the skill match multiplier for a given pet type and skill.
     *
     * Rules:
     * - BINGO (primary skill ALL): always ×1.0
     * - OWL (primary skill TAMING): always ×0.33 (never gets full match)
     * - Same skill as primary: ×1.0
     * - Different skill: ×0.33
     * - Alchemy/Enchanting mismatch: ×0.08333
     * - Mining/Fishing: already handled in skillBonus, not here
     */
    public static double getMatchMultiplier(String petType, SkillType skill) {
        if (skill == null) return 0.33;

        PetConstants constants = PetConstants.getInstance();
        constants.ensureLoaded();
        SkillType primary = constants.getPrimarySkill(petType);

        // BINGO: "ALL" skill means it matches everything
        if ("BINGO".equals(petType)) return 1.0;

        // OWL: primary is TAMING, which means it never matches any normal skill
        if ("OWL".equals(petType)) return 0.33;

        // Pet not in constants → default to mismatch
        if (primary == null) return 0.33;

        // Direct match
        if (skill == primary) return 1.0;

        // Alchemy/Enchanting penalty
        if (skill == SkillType.ALCHEMY || skill == SkillType.ENCHANTING) return 1.0 / 12.0; // ~0.08333

        // General mismatch
        return 1.0 / 3.0; // ~0.33333
    }

    /**
     * Taming multiplier: 1 + level/100 (e.g. Lvl 60 → ×1.60).
     */
    public static double getTamingMultiplier(int tamingLevel) {
        return 1.0 + tamingLevel / 100.0;
    }

    /**
     * Total exp share percentage from Taming base + global bonuses.
     * Per-pet bonuses (Exp Share item, Why Not More) are handled in calcSharedPetXP.
     */
    public static double getTotalSharePercent(PlayerPetState state) {
        return state.getTotalSharePercent();
    }
}
