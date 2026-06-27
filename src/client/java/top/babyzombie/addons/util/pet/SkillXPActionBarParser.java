package top.babyzombie.addons.util.pet;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.pet.state.PlayerPetState;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Hypixel SkyBlock skill XP messages from the action bar (overlay).
 *
 * Formats:
 *   +654.5 Foraging (11.51%)        ← percentage (non-maxed)
 *   +1,250 Combat (5,230,000/0)     ← total XP (maxed)
 *
 * For percentage: converts using skill level + cumulative XP table.
 * For maxed: tracks cumulative +XP delta.
 */
public final class SkillXPActionBarParser {

    // +XP SkillName (
    private static final Pattern HEAD = Pattern.compile(
        "\\+([0-9,.]+)\\s+([A-Za-z]+(?:\\s+[A-Za-z]+)?)\\s*\\("
    );
    // (xx.xx%)
    private static final Pattern PCT = Pattern.compile("\\(([0-9,.]+)%\\)");
    // (total/0)
    private static final Pattern MAXED = Pattern.compile("\\(([0-9,.]+)/0\\)");

    /** Last absolute XP position per skill (for delta). */
    private final Map<SkillType, Long> lastAbsolute = new EnumMap<>(SkillType.class);
    /** Fallback: last cumulative +XP per skill. */
    private final Map<SkillType, Double> lastCumulative = new EnumMap<>(SkillType.class);
    private PlayerPetState state;

    public record SkillXPEvent(SkillType skill, double xpGained) {}

    public void setState(PlayerPetState state) {
        this.state = state;
    }

    @Nullable
    public SkillXPEvent parse(Component message) {
        if (state == null) return null;
        String text = ChatUtils.stripColor(message.getString());

        Matcher hm = HEAD.matcher(text);
        if (!hm.find()) return null;

        String skillName = hm.group(2).trim();
        SkillType skill = SkillType.fromDisplayName(skillName);
        if (skill == null) return null;

        // Get the text after the '(' for sub-pattern matching
        String tail = text.substring(hm.end() - 1);

        // Try percentage format: (11.51%)
        Matcher pm = PCT.matcher(tail);
        if (pm.find()) {
            try {
                double pct = Double.parseDouble(pm.group(1).replace(",", ""));
                int level = state.getSkillLevel(skill);
                // absolute = cumulative to reach start of this level + pct * XP for this level
                long base = PetConstants.getInstance().getCumulativeXp(skill, level - 1);
                int[] table = PetConstants.getInstance().getLevelingXp();
                if (skill == SkillType.RUNECRAFTING) table = PetConstants.getInstance().getRunecraftingXp();
                else if (skill == SkillType.SOCIAL) table = PetConstants.getInstance().getSocialXp();
                else if (skill == SkillType.DUNGEONEERING) table = PetConstants.getInstance().getCatacombsXp();
                if (table == null || level < 1 || level > table.length) return null;
                long xpForLevel = table[level - 1];
                long absolute = base + Math.round(pct / 100.0 * xpForLevel);
                return delta(skill, absolute);
            } catch (NumberFormatException e) { return null; }
        }

        // Try maxed format: (total/0)
        Matcher mm = MAXED.matcher(tail);
        if (mm.find()) {
            try {
                long absolute = Long.parseLong(mm.group(1).replace(",", ""));
                return delta(skill, absolute);
            } catch (NumberFormatException e) { return null; }
        }

        // Fallback: cumulative +XP delta
        try {
            double cumulative = Double.parseDouble(hm.group(1).replace(",", ""));
            Double prev = lastCumulative.put(skill, cumulative);
            if (prev != null) {
                double delta = cumulative - prev;
                if (delta > 0) return new SkillXPEvent(skill, delta);
            }
        } catch (NumberFormatException ignored) {}

        return null;
    }

    private SkillXPEvent delta(SkillType skill, long current) {
        Long prev = lastAbsolute.put(skill, current);
        if (prev == null) return null;
        long d = current - prev;

        // Level-up: percentage dropped → negative delta.
        // Try incrementing stored level until the math works.
        if (d <= 0) {
            int maxLevel = 60;
            for (int tryLvl = state.getSkillLevel(skill) + 1; tryLvl <= maxLevel; tryLvl++) {
                state.skillLevels.put(skill, tryLvl);
                // Could recompute absolute with new level here, but missing the pct.
                // Just update the stored level and skip this tick.
                PetManager.getInstance().saveCurrentProfile();
                break;
            }
            return null;
        }

        return new SkillXPEvent(skill, d);
    }

    public void reset() {
        lastAbsolute.clear();
        lastCumulative.clear();
    }
}
