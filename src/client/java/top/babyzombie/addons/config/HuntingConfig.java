package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;
import io.github.notenoughupdates.moulconfig.ChromaColour;

public class HuntingConfig {

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.group.safari", desc = "")
    @Accordion
    public Safari safari = new Safari();

    public static class Safari {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariBellDisplay", desc = "config.babyzombieaddons.option.safariBellDisplay.desc") @ConfigEditorBoolean @SearchTag("safari")
        public boolean bellDisplay = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariShulkerGlow", desc = "config.babyzombieaddons.option.safariShulkerGlow.desc") @ConfigEditorBoolean @SearchTag("safari")
        public boolean shulkerGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariShulkerGlowColor", desc = "config.babyzombieaddons.option.safariShulkerGlowColor.desc") @ConfigEditorColour @SearchTag("safari")
        public ChromaColour shulkerGlowColor = ChromaColour.fromStaticRGB(0, 255, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHideyhoGlow", desc = "config.babyzombieaddons.option.safariHideyhoGlow.desc") @ConfigEditorBoolean @SearchTag("safari")
        public boolean hideyhoGlow = false;
    }
}
