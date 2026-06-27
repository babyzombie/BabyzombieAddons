package top.babyzombie.addons.util.pet;

import org.jetbrains.annotations.Nullable;
import java.util.Locale;

/**
 * Hypixel SkyBlock skill types used for pet XP calculation.
 */
public enum SkillType {
    FARMING,
    MINING,
    COMBAT,
    FORAGING,
    FISHING,
    ENCHANTING,
    ALCHEMY,
    TAMING,
    CARPENTRY,
    RUNECRAFTING,
    SOCIAL,
    DUNGEONEERING,
    HUNTING;

    /**
     * Parse from the display name shown in the action bar (e.g. "Farming" → FARMING).
     * Case-insensitive.
     */
    @Nullable
    public static SkillType fromDisplayName(String name) {
        if (name == null) return null;
        String upper = name.trim().toUpperCase(Locale.ROOT);
        try {
            return valueOf(upper);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
