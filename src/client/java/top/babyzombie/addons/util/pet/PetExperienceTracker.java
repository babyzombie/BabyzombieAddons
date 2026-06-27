package top.babyzombie.addons.util.pet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.pet.state.PlayerPetState;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central tracker: listens to action bar skill XP messages, calculates pet XP
 * for summoned and shared pets, and updates PetManager data.
 */
public final class PetExperienceTracker {

    /** Pattern to extract XP bonus from pet item lore: "Gives +40% pet exp for Farming." */
    private static final Pattern ITEM_XP_PATTERN = Pattern.compile(
        "Gives \\+([0-9.]+)% pet exp for (.+?)\\."
    );

    /** Pattern for "Gives +10% pet exp for all skills." */
    private static final Pattern ITEM_XP_ALL_PATTERN = Pattern.compile(
        "Gives \\+([0-9.]+)% pet exp for all skills\\."
    );

    private static final PetExperienceTracker INSTANCE = new PetExperienceTracker();
    private final SkillXPActionBarParser actionBarParser = new SkillXPActionBarParser();
    private PetManager petManager;
    private PlayerPetState state;
    private boolean initialized;

    // Last calculation result for debug display
    @Nullable
    private LastCalculation lastCalculation;

    /** Record for debug display of last XP calculation. */
    public static final class LastCalculation {
        public final String skillName;
        public final double skillXp;
        public final String summonedPetType;
        public final double summonedXp;
        public final List<SharedPetXpResult> sharedResults;

        public LastCalculation(String skillName, double skillXp, String summonedPetType, double summonedXp,
                               List<SharedPetXpResult> sharedResults) {
            this.skillName = skillName;
            this.skillXp = skillXp;
            this.summonedPetType = summonedPetType;
            this.summonedXp = summonedXp;
            this.sharedResults = sharedResults;
        }
    }

    public record SharedPetXpResult(String petType, double xpGained) {}

    private PetExperienceTracker() {}

    public static PetExperienceTracker getInstance() { return INSTANCE; }

    public void init(PetManager petManager, PlayerPetState state) {
        if (initialized) return;
        this.petManager = petManager;
        this.state = state;
        this.actionBarParser.setState(state);
        initialized = true;

        // Listen to overlay (action bar) messages
        ClientReceiveMessageEvents.ALLOW_GAME.register(this::onGameMessage);
    }

    @Nullable
    public LastCalculation getLastCalculation() {
        return lastCalculation;
    }

    /** Skills that actually generate pet XP. */
    private static final Set<SkillType> PET_XP_SKILLS = Set.of(
        SkillType.FARMING, SkillType.MINING, SkillType.COMBAT,
        SkillType.FORAGING, SkillType.FISHING, SkillType.ENCHANTING,
        SkillType.ALCHEMY, SkillType.TAMING
    );

    private boolean onGameMessage(Component message, boolean overlay) {
        if (!overlay) return true;
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return true;
        if (petManager == null || state == null) return true;

        SkillXPActionBarParser.SkillXPEvent event = actionBarParser.parse(message);
        if (event == null) return true;
        if (!PET_XP_SKILLS.contains(event.skill())) return true;

        onSkillXP(event.skill(), event.xpGained());
        return true;
    }

    /**
     * Process a skill XP gain event. Calculates and distributes XP to pets.
     */
    public void onSkillXP(SkillType skill, double skillXP) {
        PetData currentPet = petManager.getCurrentPet();
        if (currentPet == null) return;

        // Calculate summoned pet XP
        double petItemMult = resolvePetItemMultiplier(currentPet.heldItem(), skill);
        double summonedXP = round2(PetXPCalculator.calcSummonedPetXP(
            skillXP, skill, currentPet.type(),
            state.getTamingLevel(),
            petItemMult,
            state.beastmasterMult,
            state.battleExperienceLevel,
            state.dianaPetXpBuff
        ));

        petManager.addPetExp(currentPet.uuid(), summonedXP);
        // Calculate shared pet XP
        List<PetData> sharedPets = petManager.getSharedPets();
        java.util.ArrayList<SharedPetXpResult> sharedResults = new java.util.ArrayList<>();

        // Without Diana's "Sharing is Caring" perk, only slot 1 (first in list) is active
        int activeSlots = state.dianaSharingIsCaring ? 3 : 1;
        int slotCount = 0;

        for (PetData sharedPet : sharedPets) {
            if (slotCount >= activeSlots) break;
            if (sharedPet.uuid() == null) continue;
            if (sharedPet.uuid().equals(currentPet.uuid())) continue; // Skip if summoned pet is also in share slot

            slotCount++;

            double sharedPetItemMult = resolvePetItemMultiplier(sharedPet.heldItem(), skill);
            boolean hasExpShare = "PET_ITEM_EXP_SHARE".equals(sharedPet.heldItem());

            double sharedXP = round2(PetXPCalculator.calcSharedPetXP(
                summonedXP,
                currentPet.type(), petItemMult,
                sharedPet.type(), sharedPetItemMult,
                skill,
                state.getTamingLevel(),
                hasExpShare,
                state.dianaSharingIsCaring,
                state.whyNotMoreLevel
            ));

            petManager.addPetExp(sharedPet.uuid(), sharedXP);
            sharedResults.add(new SharedPetXpResult(sharedPet.type(), sharedXP));
        }

        // Store for debug display
        lastCalculation = new LastCalculation(
            skill.name(), round2(skillXP), currentPet.type(), summonedXP, sharedResults
        );
    }

    // ===== Pet Item Multiplier Resolution =====

    /**
     * Look up a held item's XP bonus multiplier for a given skill from the item repo lore.
     * Returns 1.0 if no bonus found, or 1 + percentage/100 if found.
     */
    public static double resolvePetItemMultiplier(@Nullable String heldItem, SkillType skill) {
        if (heldItem == null) return 1.0;
        Path itemsDir = PetConstants.getInstance().getItemsDir();
        if (itemsDir == null) return 1.0;

        Path file = itemsDir.resolve(heldItem + ".json");
        if (!Files.exists(file)) return 1.0;

        try {
            String raw = Files.readString(file);
            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
            var loreArr = obj.getAsJsonArray("lore");
            if (loreArr == null) return 1.0;

            for (var elem : loreArr) {
                String line = ChatUtils.stripColor(elem.getAsString());

                // Check "Gives +X% pet exp for {Skill}."
                Matcher m = ITEM_XP_PATTERN.matcher(line);
                if (m.find()) {
                    String boostSkillName = m.group(2).trim();
                    SkillType boostSkill = SkillType.fromDisplayName(boostSkillName);
                    if (boostSkill == skill) {
                        double pct = Double.parseDouble(m.group(1));
                        return 1.0 + pct / 100.0;
                    }
                }

                // Check "Gives +X% pet exp for all skills."
                Matcher mAll = ITEM_XP_ALL_PATTERN.matcher(line);
                if (mAll.find()) {
                    double pct = Double.parseDouble(mAll.group(1));
                    return 1.0 + pct / 100.0;
                }
            }
        } catch (IOException | NumberFormatException ignored) {}

        return 1.0;
    }

    /** Round to 2 decimal places to avoid floating-point noise. */
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /**
     * Reset tracked state (on profile switch).
     */
    public void reset() {
        actionBarParser.reset();
        lastCalculation = null;
    }
}
