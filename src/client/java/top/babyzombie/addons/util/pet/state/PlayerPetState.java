package top.babyzombie.addons.util.pet.state;

import top.babyzombie.addons.util.pet.SkillType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-profile persisted state for pet XP calculation.
 * Saved alongside PetManager data as pet_state.json.
 */
public final class PlayerPetState {
    // ---- Skills ----
    /** All tracked skill levels (from "Your Skills" page). Default Taming = 1. */
    public Map<SkillType, Integer> skillLevels = new EnumMap<>(SkillType.class);
    {
        skillLevels.put(SkillType.TAMING, 1);
    }

    // For backward compat with old pet_state.json that has tamingLevel field
    // Gson will set this, then we migrate in PetManager.loadProfile
    public transient int tamingLevel;

    // ---- Exp Sharing ----
    // UUIDs are stored in PetManager's pets.json, not here.

    // ---- Beastmaster Crest ----
    public double beastmasterMult = 1.0;

    // ---- Battle Experience ----
    public int battleExperienceLevel = 0;

    // ---- Mayor ----
    public boolean dianaMayor;
    public boolean dianaPetXpBuff;
    public boolean dianaSharingIsCaring;
    public long mayorLastCheckTime;

    // ===== Helpers =====

    public int getTamingLevel() {
        return skillLevels.getOrDefault(SkillType.TAMING, 1);
    }

    public int getSkillLevel(SkillType skill) {
        return skillLevels.getOrDefault(skill, 1);
    }

    public double getTamingMultiplier() {
        return 1.0 + getTamingLevel() / 100.0;
    }

    public double getTamingSharePercent() {
        return getTamingLevel() * 0.2;
    }

    public double getTotalSharePercent() {
        double pct = getTamingSharePercent();
        pct += dianaSharingIsCaring ? 10.0 : 0.0;
        return pct;
    }

    public double getBattleExperienceMultiplier() {
        return 1.0 + battleExperienceLevel / 100.0;
    }
}
