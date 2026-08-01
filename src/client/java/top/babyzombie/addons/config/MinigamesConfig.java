package top.babyzombie.addons.config;

import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;

public class MinigamesConfig {

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.group.ravengard", desc = "")
    @Accordion
    public Ravengard ravengard = new Ravengard();

    public static class Ravengard {
        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.ravengardPriceDisplay", desc = "config.babyzombieaddons.option.ravengardPriceDisplay.desc")
        @ConfigEditorBoolean
        @SearchTag("crown") @SearchTag("price")
        public boolean priceDisplay = false;
    }
}
