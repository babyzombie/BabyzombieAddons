package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;

public class GardenConfig {

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.pestDisplay", desc = "config.babyzombieaddons.option.pestDisplay.desc") @ConfigEditorBoolean @SearchTag("pest")
    public boolean pestDisplay = false;

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.xpOrbSoundRemoval", desc = "config.babyzombieaddons.option.xpOrbSoundRemoval.desc") @ConfigEditorSlider(minValue = 0, maxValue = 100, minStep = 1) @SearchTag("xp")
    public int xpOrbSoundRemoval = 100;

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.signAutoRotate", desc = "config.babyzombieaddons.option.signAutoRotate.desc") @ConfigEditorBoolean
    public boolean signAutoRotate = false;

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.farmingToolSwingSuppression", desc = "config.babyzombieaddons.option.farmingToolSwingSuppression.desc") @ConfigEditorBoolean
    public boolean farmingToolSwingSuppression = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.group.trevor", desc = "")
    @Accordion
    public Trevor trevor = new Trevor();

    public static class Trevor {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.trevorAutoAccept", desc = "config.babyzombieaddons.option.trevorAutoAccept.desc") @ConfigEditorBoolean @SearchTag("trevor")
        public boolean autoAccept = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.trevorAutoCall", desc = "config.babyzombieaddons.option.trevorAutoCall.desc") @ConfigEditorBoolean @SearchTag("trevor")
        public boolean autoCall = false;
    }
}
