package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
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
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariBellDisplay", desc = "config.babyzombieaddons.option.safariBellDisplay.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("rainbowbug")
        public boolean bellDisplay = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariShulkerGlow", desc = "config.babyzombieaddons.option.safariShulkerGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("hideonfloor") @SearchTag("hideonwall")
        public boolean shulkerGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariShulkerGlowColor", desc = "config.babyzombieaddons.option.safariShulkerGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("hideonfloor") @SearchTag("hideonwall")
        public ChromaColour shulkerGlowColor = ChromaColour.fromStaticRGB(0, 255, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHideyhoGlow", desc = "config.babyzombieaddons.option.safariHideyhoGlow.desc") @ConfigEditorBoolean @SearchTag("safari")
        public boolean hideyhoGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariWumpaRecord", desc = "config.babyzombieaddons.option.safariWumpaRecord.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("wumpa")
        public boolean wumpaRecord = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariWardenGlow", desc = "config.babyzombieaddons.option.safariWardenGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("warden")
        public boolean wardenGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariWardenCooldownTicks", desc = "config.babyzombieaddons.option.safariWardenCooldownTicks.desc") @ConfigEditorSlider(minValue = 50, maxValue = 300, minStep = 1) @SearchTag("safari") @SearchTag("warden")
        public int wardenCooldownTicks = 140;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariWardenGlowCooldownColor", desc = "config.babyzombieaddons.option.safariWardenGlowCooldownColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("warden")
        public ChromaColour wardenGlowCooldownColor = ChromaColour.fromStaticRGB(255, 80, 80, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariWardenGlowReadyColor", desc = "config.babyzombieaddons.option.safariWardenGlowReadyColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("warden")
        public ChromaColour wardenGlowReadyColor = ChromaColour.fromStaticRGB(80, 255, 80, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariSculkSensorGlow", desc = "config.babyzombieaddons.option.safariSculkSensorGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("warden")
        public boolean sculkSensorGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariSculkSensorGlowColor", desc = "config.babyzombieaddons.option.safariSculkSensorGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("warden")
        public ChromaColour sculkSensorGlowColor = ChromaColour.fromStaticRGB(170, 0, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariBatGlow", desc = "config.babyzombieaddons.option.safariBatGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("bat")
        public boolean batGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariBatGlowColor", desc = "config.babyzombieaddons.option.safariBatGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("bat")
        public ChromaColour batGlowColor = ChromaColour.fromStaticRGB(255, 255, 85, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariDuplicoGlow", desc = "config.babyzombieaddons.option.safariDuplicoGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("duplico")
        public boolean duplicoGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariDuplicoGlowColor", desc = "config.babyzombieaddons.option.safariDuplicoGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("duplico")
        public ChromaColour duplicoGlowColor = ChromaColour.fromStaticRGB(85, 255, 85, 255);
        @Expose
        @ConfigOption(name = "config.babyzombieaddons.group.safariHunterTrade", desc = "")
        @Accordion
        public HunterTrade hunterTrade = new HunterTrade();
    }

    /** 猎手交易（组中组） */
    public static class HunterTrade {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHunterTrade", desc = "config.babyzombieaddons.option.safariHunterTrade.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("hunter") @SearchTag("shard")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHunterTradePopup", desc = "config.babyzombieaddons.option.safariHunterTradePopup.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("hunter")
        public boolean popup = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHunterTradeParty", desc = "config.babyzombieaddons.option.safariHunterTradeParty.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("hunter")
        public boolean party = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHunterTradeWorldText", desc = "config.babyzombieaddons.option.safariHunterTradeWorldText.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("hunter")
        public boolean worldText = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHunterTradeHud", desc = "config.babyzombieaddons.option.safariHunterTradeHud.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("hunter")
        public boolean hud = true;
    }

    public static class TorrhusCanyon {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.beeheemothHighlight", desc = "config.babyzombieaddons.option.beeheemothHighlight.desc") @ConfigEditorDropdown @SearchTag("torrhus")
        public BeeheemothHighlightMode highlightMode = BeeheemothHighlightMode.OFF;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.beeheemothGlowColor", desc = "config.babyzombieaddons.option.beeheemothGlowColor.desc") @ConfigEditorColour @SearchTag("torrhus")
        public ChromaColour glowColor = ChromaColour.fromStaticRGB(170, 0, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.templePuzzle", desc = "config.babyzombieaddons.option.templePuzzle.desc") @ConfigEditorBoolean @SearchTag("torrhus") @SearchTag("temple")
        public boolean templePuzzle = false;
    }
}
