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
 *   +654.5 Foraging (11.51%)        ← percentage (mid-level, non-maxed)
 *   +1,250 Combat (5,230,000/0)     ← total XP (maxed)
 *   +50 Farming (1,234/5,000)       ← currentXP/nextLevelXP (low-level)
 *
 * Deltas are computed from consecutive progress samples within the same format.
 * Level-ups are detected by a percentage drop or currentXP reset and the
 * bridging XP (remainder of old level + progress into new level) is included.
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
    // (currentXP/nextLevelXP) — low-level skills that haven't reached percentage display yet
    private static final Pattern XP_PROGRESS = Pattern.compile("\\(([0-9,.]+)/([0-9,.]+)\\)");

    // ── Per-skill progress tracking ──

    private enum Fmt { PROGRESS, PCT, MAXED }

    /** A snapshot of the progress read from the last action bar for a skill. */
    private static final class Snap {
        final Fmt fmt;
        final double val;   // currentXP, or percentage, or total
        final double aux;   // nextLevelXP, or xpForLevel, or 0 (unused)
        final int level;    // state skill level when this snap was taken

        Snap(Fmt fmt, double val, double aux, int level) {
            this.fmt = fmt; this.val = val; this.aux = aux; this.level = level;
        }
    }

    private final Map<SkillType, Snap> snap = new EnumMap<>(SkillType.class);
    private PlayerPetState state;

    public record SkillXPEvent(SkillType skill, double xpGained) {}

    public void setState(PlayerPetState state) {
        this.state = state;
        // Restore persisted snap baseline so the first XP event after restart
        // still has a previous sample to compute a delta against.
        restoreFromState();
    }

    // ── Parse ──

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
            return handlePct(skill, pm);
        }

        // Try maxed format: (total/0)
        Matcher mm = MAXED.matcher(tail);
        if (mm.find()) {
            return handleMaxed(skill, mm);
        }

        // Try progress format: (currentXP/nextLevelXP) — low-level skills
        Matcher xm = XP_PROGRESS.matcher(tail);
        if (xm.find()) {
            return handleProgress(skill, xm);
        }

        return null;
    }

    // ── Format handlers ──

    @Nullable
    private SkillXPEvent handlePct(SkillType skill, Matcher pm) {
        double pct;
        try {
            pct = Double.parseDouble(pm.group(1).replace(",", ""));
        } catch (NumberFormatException e) { return null; }

        int level = state.getSkillLevel(skill);
        int[] table = getTable(skill);
        if (table == null || level < 1 || level > table.length) return null;
        long xpForLevel = table[level - 1];

        Snap prev = snap.get(skill);
        if (prev == null) {
            snap.put(skill, new Snap(Fmt.PCT, pct, xpForLevel, level));
            persistToState();
            return null;
        }
        if (prev.fmt != Fmt.PCT) {
            return transitionToPct(skill, prev, pct, xpForLevel, level, table);
        }

        double d;
        if (pct >= prev.val) {
            // Same level: normal progress
            d = (pct - prev.val) / 100.0 * xpForLevel;
        } else {
            // Level-up: percentage dropped.
            d = ((100.0 - prev.val) / 100.0 * prev.aux) + (pct / 100.0 * xpForLevel);
            // Multi-level jump (milestone rewards)
            double remaining = d - xpForLevel;
            while (remaining >= 0 && level < table.length) {
                level++;
                remaining -= table[level - 1];
            }
            state.skillLevels.put(skill, level);
            PetManager.getInstance().saveCurrentProfile();
        }

        snap.put(skill, new Snap(Fmt.PCT, pct, xpForLevel, level));
        persistToState();
        if (d > 0) return new SkillXPEvent(skill, d);
        return null;
    }

    /** Handle transition from any other format into PCT. */
    @Nullable
    private SkillXPEvent transitionToPct(SkillType skill, Snap prev, double pct,
                                          long xpForLevel, int curLevel, int[] table) {
        double d = transitionDelta(skill, prev, pct, xpForLevel, curLevel, true, table);
        if (d >= 0) {
            snap.put(skill, new Snap(Fmt.PCT, pct, xpForLevel, curLevel));
            persistToState();
            if (d > 0) return new SkillXPEvent(skill, d);
        } else {
            // Couldn't resolve — baseline reset
            snap.put(skill, new Snap(Fmt.PCT, pct, xpForLevel, curLevel));
            persistToState();
        }
        return null;
    }

    /**
     * Compute XP delta across a format transition.
     * @param newFmtIsPct true if transitioning INTO PCT, false if into PROGRESS or other
     * @return delta, or -1 if unresolvable (should reset baseline)
     */
    private double transitionDelta(SkillType skill, Snap prev, double newVal, double newAux,
                                    int curLevel, boolean newFmtIsPct, int[] table) {
        if (prev.fmt == Fmt.PROGRESS && newFmtIsPct) {
            // PROGRESS → PCT
            if (prev.level == curLevel) {
                // Same level: convert old cur/next to equivalent pct
                double eqPct = (prev.val / prev.aux) * 100.0;
                return (newVal - eqPct) / 100.0 * newAux;
            } else {
                // Level-up: complete old level + progress into new level
                double d = (prev.aux - prev.val) + (newVal / 100.0 * newAux);
                double remaining = d - newAux;
                int lvl = curLevel;
                while (remaining >= 0 && lvl < table.length) {
                    lvl++;
                    remaining -= table[lvl - 1];
                }
                if (lvl != curLevel) {
                    state.skillLevels.put(skill, lvl);
                    PetManager.getInstance().saveCurrentProfile();
                }
                return d;
            }
        }

        if (prev.fmt == Fmt.PCT && !newFmtIsPct) {
            // PCT → PROGRESS
            if (prev.level == curLevel) {
                // Same level: convert old pct to equivalent cur
                double eqCur = (prev.val / 100.0) * prev.aux;
                return newVal - eqCur;
            } else {
                // Level-up
                return ((100.0 - prev.val) / 100.0 * prev.aux) + newVal;
            }
        }

        return -1; // Unresolvable combination
    }

    @Nullable
    private SkillXPEvent handleProgress(SkillType skill, Matcher xm) {
        double cur, next;
        try {
            cur = Double.parseDouble(xm.group(1).replace(",", ""));
            next = Double.parseDouble(xm.group(2).replace(",", ""));
        } catch (NumberFormatException e) { return null; }

        int level = state.getSkillLevel(skill);

        Snap prev = snap.get(skill);
        if (prev == null) {
            snap.put(skill, new Snap(Fmt.PROGRESS, cur, next, level));
            persistToState();
            return null;
        }
        if (prev.fmt != Fmt.PROGRESS) {
            // Transition from another format (rare — normally PCT→PROGRESS
            // shouldn't happen, but handle cleanly)
            double d = transitionDelta(skill, prev, cur, next, level, false, getTable(skill));
            snap.put(skill, new Snap(Fmt.PROGRESS, cur, next, level));
            persistToState();
            if (d > 0) return new SkillXPEvent(skill, d);
            return null;
        }

        double d;
        if (cur >= prev.val) {
            d = cur - prev.val;
        } else {
            // Level-up: currentXP reset
            d = (prev.aux - prev.val) + cur;
            state.skillLevels.put(skill, level + 1);
            PetManager.getInstance().saveCurrentProfile();
        }

        snap.put(skill, new Snap(Fmt.PROGRESS, cur, next, level));
        persistToState();
        if (d > 0) return new SkillXPEvent(skill, d);
        return null;
    }

    @Nullable
    private SkillXPEvent handleMaxed(SkillType skill, Matcher mm) {
        double total;
        try {
            total = Double.parseDouble(mm.group(1).replace(",", ""));
        } catch (NumberFormatException e) { return null; }

        int level = state.getSkillLevel(skill);

        Snap prev = snap.get(skill);
        if (prev == null) {
            snap.put(skill, new Snap(Fmt.MAXED, total, 0, level));
            persistToState();
            return null;
        }
        if (prev.fmt != Fmt.MAXED) {
            // Transition to MAXED — most commonly from PCT.
            // Need to compute absolute position from old format to compare with total.
            double d = -1;
            if (prev.fmt == Fmt.PCT) {
                long base = PetConstants.getInstance().getCumulativeXp(skill, prev.level);
                double oldAbs = base + (prev.val / 100.0) * prev.aux;
                d = total - oldAbs;
            } else if (prev.fmt == Fmt.PROGRESS) {
                long base = PetConstants.getInstance().getCumulativeXp(skill, prev.level);
                double oldAbs = base + prev.val;
                d = total - oldAbs;
            }
            snap.put(skill, new Snap(Fmt.MAXED, total, 0, level));
            persistToState();
            if (d > 0) return new SkillXPEvent(skill, d);
            return null;
        }

        double d = total - prev.val;
        snap.put(skill, new Snap(Fmt.MAXED, total, 0, level));
        persistToState();
        if (d > 0) return new SkillXPEvent(skill, d);
        return null;
    }

    // ── Persistence (through PlayerPetState → pet_state.json) ──

    /** Restore snap baseline from state loaded from disk. */
    private void restoreFromState() {
        if (state == null) return;
        for (var e : state.snapFmt.entrySet()) {
            SkillType skill = SkillType.fromDisplayName(e.getKey());
            if (skill == null) continue;
            Fmt fmt;
            try { fmt = Fmt.valueOf(e.getValue()); } catch (IllegalArgumentException ex) { continue; }
            Double val = state.snapVal.get(e.getKey());
            Double aux = state.snapAux.get(e.getKey());
            Integer lvl = state.snapLevel.get(e.getKey());
            if (val == null || aux == null || lvl == null) continue;
            snap.put(skill, new Snap(fmt, val, aux, lvl));
        }
    }

    /** Write current snap to state so it gets persisted on next profile save. */
    private void persistToState() {
        if (state == null) return;
        state.snapFmt.clear();
        state.snapVal.clear();
        state.snapAux.clear();
        state.snapLevel.clear();
        for (var e : snap.entrySet()) {
            String key = e.getKey().name();
            state.snapFmt.put(key, e.getValue().fmt.name());
            state.snapVal.put(key, e.getValue().val);
            state.snapAux.put(key, e.getValue().aux);
            state.snapLevel.put(key, e.getValue().level);
        }
    }

    // ── Helpers ──

    private static int[] getTable(SkillType skill) {
        return switch (skill) {
            case RUNECRAFTING -> PetConstants.getInstance().getRunecraftingXp();
            case SOCIAL -> PetConstants.getInstance().getSocialXp();
            case DUNGEONEERING -> PetConstants.getInstance().getCatacombsXp();
            default -> PetConstants.getInstance().getLevelingXp();
        };
    }

    public void reset() {
        snap.clear();
        if (state != null) {
            state.snapFmt.clear();
            state.snapVal.clear();
            state.snapAux.clear();
            state.snapLevel.clear();
        }
    }
}
