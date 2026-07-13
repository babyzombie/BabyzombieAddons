package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;
import top.babyzombie.addons.config.ModConfig.*;

public class KuudraConfig {

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.hpDisplay", desc = "config.babyzombieaddons.option.hpDisplay.desc") @ConfigEditorDropdown
    public HpDisplayMode hpDisplay = HpDisplayMode.OFF;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.phaseTimer", desc = "config.babyzombieaddons.option.phaseTimer.desc") @ConfigEditorBoolean @SearchTag("phase")
    public boolean phaseTimer = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.stunTimer", desc = "config.babyzombieaddons.option.stunTimer.desc") @ConfigEditorBoolean @SearchTag("stun")
    public boolean stunTimer = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.energyDisplay", desc = "config.babyzombieaddons.option.energyDisplay.desc") @ConfigEditorBoolean @SearchTag("energy")
    public boolean energyDisplay = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.boxKuudra", desc = "config.babyzombieaddons.option.boxKuudra.desc") @ConfigEditorBoolean @SearchTag("box")
    public boolean boxKuudra = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.enderPearlRefill", desc = "config.babyzombieaddons.option.enderPearlRefill.desc") @ConfigEditorBoolean @SearchTag("pearl")
    public boolean enderPearlRefill = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.followerHelmetPrice", desc = "config.babyzombieaddons.option.followerHelmetPrice.desc") @ConfigEditorBoolean @SearchTag("follower")
    public boolean followerHelmetPrice = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.muteCrimsonArmor", desc = "config.babyzombieaddons.option.muteCrimsonArmor.desc") @ConfigEditorBoolean @SearchTag("crimson")
    public boolean muteCrimsonArmor = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.nopeMagmafish", desc = "config.babyzombieaddons.option.nopeMagmafish.desc") @ConfigEditorBoolean @SearchTag("magmafish")
    public boolean nopeMagmafish = false;

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.waypoints", desc = "") @Accordion
    public Waypoints waypoints = new Waypoints();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.arrowPoison", desc = "") @Accordion
    public ArrowPoison arrowPoison = new ArrowPoison();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.perkShop", desc = "") @Accordion
    public PerkShop perkShop = new PerkShop();

    public static class Waypoints {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.supplyBeacons", desc = "config.babyzombieaddons.option.supplyBeacons.desc") @ConfigEditorBoolean @SearchTag("supply")
        public boolean supplyBeacons = false;
        @Expose
        @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.supplyBeaconColor", desc = "config.babyzombieaddons.option.supplyBeaconColor.desc")
        public String supplyBeaconColor = "0:255:0:255:0";
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.supplyDropoffBeacons", desc = "config.babyzombieaddons.option.supplyDropoffBeacons.desc") @ConfigEditorBoolean @SearchTag("supply")
        public boolean supplyDropoffBeacons = false;
        @Expose
        @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.supplyDropoffBeaconColor", desc = "config.babyzombieaddons.option.supplyDropoffBeaconColor.desc")
        public String supplyDropoffBeaconColor = "0:255:255:255:0";
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ballistaProgressText", desc = "config.babyzombieaddons.option.ballistaProgressText.desc") @ConfigEditorBoolean @SearchTag("ballista")
        public boolean ballistaProgressText = false;
        @Expose
        @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.ballistaTextColor", desc = "config.babyzombieaddons.option.ballistaTextColor.desc")
        public String ballistaTextColor = "0:255:255:255:85";
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ballistaBuildBeacons", desc = "config.babyzombieaddons.option.ballistaBuildBeacons.desc") @ConfigEditorBoolean @SearchTag("ballista")
        public boolean ballistaBuildBeacons = false;
        @Expose
        @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.ballistaBeaconColor", desc = "config.babyzombieaddons.option.ballistaBeaconColor.desc")
        public String ballistaBeaconColor = "0:255:76:127:255";
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fuelOrbBeacons", desc = "config.babyzombieaddons.option.fuelOrbBeacons.desc") @ConfigEditorBoolean @SearchTag("fuel")
        public boolean fuelOrbBeacons = false;
        @Expose
        @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.fuelOrbBeaconColor", desc = "config.babyzombieaddons.option.fuelOrbBeaconColor.desc")
        public String fuelOrbBeaconColor = "0:255:255:0:0";
    }

    public static class ArrowPoison {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.toxicArrowMinTier", desc = "config.babyzombieaddons.option.toxicArrowMinTier.desc") @ConfigEditorDropdown @SearchTag("toxic")
        public ToxicArrowMinTier toxicArrowMinTier = ToxicArrowMinTier.T3;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.toxicArrowTiming", desc = "config.babyzombieaddons.option.toxicArrowTiming.desc") @ConfigEditorDropdown @SearchTag("toxic")
        public ToxicArrowTiming toxicArrowTiming = ToxicArrowTiming.KUUDRA_STUNNED;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.toxicArrowThreshold", desc = "config.babyzombieaddons.option.toxicArrowThreshold.desc") @ConfigEditorSlider(minValue = 0, maxValue = 32, minStep = 2)
        public int toxicArrowThreshold = 0;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.toxicArrowPerMissing", desc = "config.babyzombieaddons.option.toxicArrowPerMissing.desc") @ConfigEditorSlider(minValue = 0, maxValue = 16, minStep = 1)
        public int toxicArrowPerMissing = 0;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.twilightArrowTiming", desc = "config.babyzombieaddons.option.twilightArrowTiming.desc") @ConfigEditorDropdown @SearchTag("twilight")
        public TwilightArrowTiming twilightArrowTiming = TwilightArrowTiming.P4_START;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.twilightArrowThreshold", desc = "config.babyzombieaddons.option.twilightArrowThreshold.desc") @ConfigEditorSlider(minValue = 0, maxValue = 8, minStep = 1)
        public int twilightArrowThreshold = 0;
    }

    public static class PerkShop {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.perkShopBlacklist", desc = "config.babyzombieaddons.option.perkShopBlacklist.desc") @ConfigEditorBoolean @SearchTag("perk")
        public boolean perkShopBlacklist = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.perkShopBlacklistItems", desc = "config.babyzombieaddons.option.perkShopBlacklistItems.desc") @ConfigEditorText
        public String perkShopBlacklistItems = "Elle's Pickaxe,Elle's Lava Rod,Auto Revive,Support Route,Mining Frenzy I";
    }
}
