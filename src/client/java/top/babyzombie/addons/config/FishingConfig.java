package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;

public class FishingConfig {

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.group.rareSeaCreatures", desc = "")
    @Accordion
    public RareSeaCreatures rareSeaCreatures = new RareSeaCreatures();

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.group.preventInstantReel", desc = "")
    @Accordion
    public PreventInstantReel preventInstantReel = new PreventInstantReel();

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.popupBaitLow", desc = "config.babyzombieaddons.option.popupBaitLow.desc") @ConfigEditorSlider(minValue = 0, maxValue = 64, minStep = 2) @SearchTag("bait")
    public int popupBaitLow = 0;

    public static class RareSeaCreatures {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesAlert", desc = "config.babyzombieaddons.option.rareSeaCreaturesAlert.desc") @ConfigEditorBoolean @SearchTag("rare")
        public boolean alert = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesAlertTitle", desc = "config.babyzombieaddons.option.rareSeaCreaturesAlertTitle.desc") @ConfigEditorBoolean @SearchTag("rare")
        public boolean alertTitle = false;
    }

    public static class PreventInstantReel {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.preventInstantReel", desc = "config.babyzombieaddons.option.preventInstantReel.desc") @ConfigEditorBoolean @SearchTag("reel")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.preventInstantReelDelay", desc = "config.babyzombieaddons.option.preventInstantReelDelay.desc") @ConfigEditorSlider(minValue = 50, maxValue = 500, minStep = 25) @SearchTag("reel")
        public int delay = 200;
    }
}
