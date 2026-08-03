package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;
import top.babyzombie.addons.config.ModConfig.GlaciteMineshaftPortalAction;
import top.babyzombie.addons.config.ModConfig.GlaciteMineshaftPortalSound;
import top.babyzombie.addons.config.ModConfig.MineshaftCorpseRenderMode;

public class MiningConfig {

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.miningAbilityAlerts", desc = "config.babyzombieaddons.option.miningAbilityAlerts.desc") @ConfigEditorBoolean @SearchTag("ability")
    public boolean miningAbilityAlerts = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.drillSwingSuppression", desc = "config.babyzombieaddons.option.drillSwingSuppression.desc") @ConfigEditorBoolean @SearchTag("drill")
    public boolean drillSwingSuppression = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.creeperVisibility", desc = "config.babyzombieaddons.option.creeperVisibility.desc") @ConfigEditorBoolean @SearchTag("creeper")
    public boolean creeperVisibility = false;

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.crystalHollows", desc = "") @Accordion
    public CrystalHollows crystalHollows = new CrystalHollows();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.glaciteTunnels", desc = "") @Accordion
    public GlaciteTunnels glaciteTunnels = new GlaciteTunnels();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.mithrilGourmand", desc = "") @Accordion
    public MithrilGourmand mithrilGourmand = new MithrilGourmand();

    public static class CrystalHollows {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.nucleusAutoWarp", desc = "config.babyzombieaddons.option.nucleusAutoWarp.desc") @ConfigEditorBoolean @SearchTag("nucleus")
        public boolean nucleusAutoWarp = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.crystalHollowsPassAutoRenew", desc = "config.babyzombieaddons.option.crystalHollowsPassAutoRenew.desc") @ConfigEditorBoolean @SearchTag("pass")
        public boolean passAutoRenew = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.chestMarkers", desc = "config.babyzombieaddons.option.chestMarkers.desc") @ConfigEditorBoolean @SearchTag("chest")
        public boolean chestMarkers = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.chestLineWidth", desc = "config.babyzombieaddons.option.chestLineWidth.desc") @ConfigEditorSlider(minValue = 1, maxValue = 16, minStep = 1) @SearchTag("chest") @SearchTag("line") @SearchTag("width")
        public int chestLineWidth = 3;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.getFromSacks", desc = "config.babyzombieaddons.option.getFromSacks.desc") @ConfigEditorBoolean @SearchTag("sack")
        public boolean getFromSacks = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.scathaCooldown", desc = "config.babyzombieaddons.option.scathaCooldown.desc") @ConfigEditorBoolean @SearchTag("scatha")
        public boolean scathaCooldown = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.scathaReleaseKey", desc = "config.babyzombieaddons.option.scathaReleaseKey.desc") @ConfigEditorBoolean @SearchTag("scatha")
        public boolean scathaReleaseKey = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.armadilloEnergy", desc = "config.babyzombieaddons.option.armadilloEnergy.desc") @ConfigEditorBoolean @SearchTag("armadillo")
        public boolean armadilloEnergy = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.powderMiningSounds", desc = "config.babyzombieaddons.option.powderMiningSounds.desc") @ConfigEditorBoolean @SearchTag("powder")
        public boolean powderMiningSounds = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.jungleTempleThinWall", desc = "config.babyzombieaddons.option.jungleTempleThinWall.desc") @ConfigEditorBoolean @SearchTag("jungle")
        public boolean jungleTempleThinWall = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.corleoneTimer", desc = "config.babyzombieaddons.option.corleoneTimer.desc") @ConfigEditorBoolean @SearchTag("corleone")
        public boolean corleoneTimer = false;
    }

    public static class GlaciteTunnels {
        @Expose @ConfigOption(name = "config.babyzombieaddons.group.glaciteMineshaft", desc = "") @Accordion
        public GlaciteMineshaft glaciteMineshaft = new GlaciteMineshaft();
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.mineshaftWaypoints", desc = "config.babyzombieaddons.option.mineshaftWaypoints.desc") @ConfigEditorBoolean @SearchTag("mineshaft")
        public boolean mineshaftWaypoints = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.mineshaftCorpseRenderMode", desc = "config.babyzombieaddons.option.mineshaftCorpseRenderMode.desc") @ConfigEditorDropdown @SearchTag("mineshaft") @SearchTag("corpse") @SearchTag("render") @SearchTag("box")
        public MineshaftCorpseRenderMode mineshaftCorpseRenderMode = MineshaftCorpseRenderMode.WIREFRAME;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.suspiciousScrapCounter", desc = "config.babyzombieaddons.option.suspiciousScrapCounter.desc") @ConfigEditorBoolean @SearchTag("scrap")
        public boolean suspiciousScrapCounter = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.greatGlaciteWaypoints", desc = "config.babyzombieaddons.option.greatGlaciteWaypoints.desc") @ConfigEditorBoolean @SearchTag("glacite")
        public boolean greatGlaciteWaypoints = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.lanternReminder", desc = "config.babyzombieaddons.option.lanternReminder.desc") @ConfigEditorBoolean @SearchTag("mineshaft") @SearchTag("lantern")
        public boolean lanternReminder = false;

        public static class GlaciteMineshaft {
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.glaciteMineshaftPortalTitle", desc = "config.babyzombieaddons.option.glaciteMineshaftPortalTitle.desc") @ConfigEditorBoolean @SearchTag("mineshaft") @SearchTag("title")
            public boolean portalTitleAlert = false;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.glaciteMineshaftPortalSound", desc = "config.babyzombieaddons.option.glaciteMineshaftPortalSound.desc") @ConfigEditorBoolean @SearchTag("mineshaft") @SearchTag("sound")
            public boolean portalSoundAlert = false;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.glaciteMineshaftPortalSoundSelect", desc = "config.babyzombieaddons.option.glaciteMineshaftPortalSoundSelect.desc") @ConfigEditorDropdown @SearchTag("mineshaft") @SearchTag("sound")
            public GlaciteMineshaftPortalSound portalSound = GlaciteMineshaftPortalSound.DRAGON;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.glaciteMineshaftPortalAction", desc = "config.babyzombieaddons.option.glaciteMineshaftPortalAction.desc") @ConfigEditorDropdown @SearchTag("mineshaft") @SearchTag("warp")
            public GlaciteMineshaftPortalAction portalAction = GlaciteMineshaftPortalAction.NONE;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.glaciteMineshaftRequireTwoLapisForPtmeWarp", desc = "config.babyzombieaddons.option.glaciteMineshaftRequireTwoLapisForPtmeWarp.desc") @ConfigEditorBoolean @SearchTag("mineshaft") @SearchTag("lapis") @SearchTag("warp")
            public boolean requireTwoLapisForPtmeWarp = false;
        }
    }

    public static class MithrilGourmand {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.mithrilGourmandAutoExpresso", desc = "config.babyzombieaddons.option.mithrilGourmandAutoExpresso.desc") @ConfigEditorBoolean @SearchTag("gourmand")
        public boolean autoExpresso = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.mithrilGourmandTriggerSeconds", desc = "config.babyzombieaddons.option.mithrilGourmandTriggerSeconds.desc") @ConfigEditorSlider(minValue = 3, maxValue = 20, minStep = 1) @SearchTag("gourmand") @SearchTag("trigger") @SearchTag("seconds")
        public int triggerSeconds = 10;
    }
}
