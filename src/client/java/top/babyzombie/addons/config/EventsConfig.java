package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;
import top.babyzombie.addons.config.ModConfig.*;

public class EventsConfig {

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.greatSpook", desc = "") @Accordion
    public GreatSpook greatSpook = new GreatSpook();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.carnival", desc = "") @Accordion
    public Carnival carnival = new Carnival();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.fruitDiggingSolver", desc = "") @Accordion
    public FruitDiggingSolver fruitDiggingSolver = new FruitDiggingSolver();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.anniversary", desc = "") @Accordion
    public Anniversary anniversary = new Anniversary();

    public static class GreatSpook {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.greatSpook", desc = "config.babyzombieaddons.option.greatSpook.desc") @ConfigEditorBoolean @SearchTag("spook")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.greatSpookDelay", desc = "config.babyzombieaddons.option.greatSpookDelay.desc") @ConfigEditorBoolean @SearchTag("spook")
        public boolean delay = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.publicSpeakingDemon", desc = "config.babyzombieaddons.option.publicSpeakingDemon.desc") @ConfigEditorText @SearchTag("spook")
        public String publicSpeakingDemon = "";
    }

    public static class Carnival {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fruitDiggingHelper", desc = "config.babyzombieaddons.option.fruitDiggingHelper.desc") @ConfigEditorBoolean @SearchTag("fruit")
        public boolean fruitDiggingHelper = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.carnivalAutoAccept", desc = "config.babyzombieaddons.option.carnivalAutoAccept.desc") @ConfigEditorBoolean @SearchTag("carnival")
        public boolean autoAccept = false;
    }

    public static class FruitDiggingSolver {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fruitDiggingSolver", desc = "config.babyzombieaddons.option.fruitDiggingSolver.desc") @ConfigEditorBoolean @SearchTag("fruit")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.solverBombPenalty", desc = "config.babyzombieaddons.option.solverBombPenalty.desc") @ConfigEditorSlider(minValue = 0, maxValue = 1000, minStep = 1) @SearchTag("solver") @SearchTag("bomb") @SearchTag("penalty")
        public float bombPenalty = 200f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.solverRumPenalty", desc = "config.babyzombieaddons.option.solverRumPenalty.desc") @ConfigEditorSlider(minValue = 0, maxValue = 1000, minStep = 1) @SearchTag("solver") @SearchTag("rum") @SearchTag("penalty")
        public float rumPenalty = 100f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.solverMinesInfoWeight", desc = "config.babyzombieaddons.option.solverMinesInfoWeight.desc") @ConfigEditorSlider(minValue = 0, maxValue = 200, minStep = 0.1f) @SearchTag("solver") @SearchTag("mines") @SearchTag("weight")
        public float minesInfoWeight = 3f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.solverTreasureInfoWeight", desc = "config.babyzombieaddons.option.solverTreasureInfoWeight.desc") @ConfigEditorSlider(minValue = 0, maxValue = 200, minStep = 0.1f) @SearchTag("solver") @SearchTag("treasure") @SearchTag("weight")
        public float treasureInfoWeight = 2f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.solverAnchorInfoWeight", desc = "config.babyzombieaddons.option.solverAnchorInfoWeight.desc") @ConfigEditorSlider(minValue = 0, maxValue = 200, minStep = 0.1f) @SearchTag("solver") @SearchTag("anchor") @SearchTag("weight")
        public float anchorInfoWeight = 1f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.solverEarlyAppleBonus", desc = "config.babyzombieaddons.option.solverEarlyAppleBonus.desc") @ConfigEditorSlider(minValue = 0, maxValue = 500, minStep = 1) @SearchTag("solver") @SearchTag("apple") @SearchTag("bonus")
        public float earlyAppleBonus = 50f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.solverEarlyCherryBonus", desc = "config.babyzombieaddons.option.solverEarlyCherryBonus.desc") @ConfigEditorSlider(minValue = 0, maxValue = 500, minStep = 1) @SearchTag("solver") @SearchTag("cherry") @SearchTag("bonus")
        public float earlyCherryBonus = 80f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.solverLateGameDigs", desc = "config.babyzombieaddons.option.solverLateGameDigs.desc") @ConfigEditorSlider(minValue = 0, maxValue = 15, minStep = 1) @SearchTag("solver") @SearchTag("lategame") @SearchTag("dig")
        public int lateGameDigs = 3;
    }

    public static class Anniversary {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.raffleTaskTracker", desc = "config.babyzombieaddons.option.raffleTaskTracker.desc") @ConfigEditorBoolean @SearchTag("raffle")
        public boolean raffleTaskTracker = false;
    }
}
