package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import net.minecraft.network.chat.Component;

public class HuntingConfig {

    public enum BeeheemothHighlightMode {
        OFF, GLOW, BEACON, BOTH;
        @Override public String toString() {
            return Component.translatable("config.babyzombieaddons.option.beeheemothHighlightMode." + name()).getString();
        }
    }

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.group.safari", desc = "")
    @Accordion
    public Safari safari = new Safari();

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.group.torrhusCanyon", desc = "")
    @Accordion
    public TorrhusCanyon torrhusCanyon = new TorrhusCanyon();

    public static class Safari {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariBellDisplay", desc = "config.babyzombieaddons.option.safariBellDisplay.desc") @ConfigEditorBoolean @SearchTag("safari")
        public boolean bellDisplay = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariShulkerGlow", desc = "config.babyzombieaddons.option.safariShulkerGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("hideonfloor") @SearchTag("hideonwall")
        public boolean shulkerGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariShulkerGlowColor", desc = "config.babyzombieaddons.option.safariShulkerGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("hideonfloor") @SearchTag("hideonwall")
        public ChromaColour shulkerGlowColor = ChromaColour.fromStaticRGB(0, 255, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHideyhoGlow", desc = "config.babyzombieaddons.option.safariHideyhoGlow.desc") @ConfigEditorBoolean @SearchTag("safari")
        public boolean hideyhoGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariSculkSensorGlow", desc = "config.babyzombieaddons.option.safariSculkSensorGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("warden")
        public boolean sculkSensorGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariSculkSensorGlowColor", desc = "config.babyzombieaddons.option.safariSculkSensorGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("warden")
        public ChromaColour sculkSensorGlowColor = ChromaColour.fromStaticRGB(170, 0, 255, 255);
    }

    public static class TorrhusCanyon {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.beeheemothHighlight", desc = "config.babyzombieaddons.option.beeheemothHighlight.desc") @ConfigEditorDropdown @SearchTag("torrhus")
        public BeeheemothHighlightMode highlightMode = BeeheemothHighlightMode.OFF;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.beeheemothGlowColor", desc = "config.babyzombieaddons.option.beeheemothGlowColor.desc") @ConfigEditorColour @SearchTag("torrhus")
        public ChromaColour glowColor = ChromaColour.fromStaticRGB(170, 0, 255, 255);
    }
}
