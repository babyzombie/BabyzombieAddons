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

    public enum ThrownCapsuleMode {
        OFF, CURRENT, UNOBSTRUCTED;

        @Override
        public String toString() {
            return Component.translatable(
                    "config.babyzombieaddons.option.safariTrajectoryThrownCapsuleMode." + name()
            ).getString();
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
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariWumpaRecord", desc = "config.babyzombieaddons.option.safariWumpaRecord.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("wumpa")
        public boolean wumpaRecord = false;

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.group.safariTrajectory", desc = "config.babyzombieaddons.group.safariTrajectory.desc")
        @Accordion
        @SearchTag("safari")
        @SearchTag("trajectory")
        public SafariTrajectory trajectory = new SafariTrajectory();

        // ── 生物群系子组：按 Safari 分区组织，每个生物独立开关 + 颜色 ──
        @Expose @ConfigOption(name = "config.babyzombieaddons.group.safariIcy", desc = "") @Accordion
        public Icy icy = new Icy();
        @Expose @ConfigOption(name = "config.babyzombieaddons.group.safariHaunted", desc = "") @Accordion
        public Haunted haunted = new Haunted();
        @Expose @ConfigOption(name = "config.babyzombieaddons.group.safariCavern", desc = "") @Accordion
        public Cavern cavern = new Cavern();
        @Expose @ConfigOption(name = "config.babyzombieaddons.group.safariForest", desc = "") @Accordion
        public Forest forest = new Forest();
        @Expose
        @ConfigOption(name = "config.babyzombieaddons.group.safariHunterTrade", desc = "")
        @Accordion
        public HunterTrade hunterTrade = new HunterTrade();
    }

    public static class SafariTrajectory {
        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.safariTrajectoryEnabled", desc = "config.babyzombieaddons.option.safariTrajectoryEnabled.desc")
        @ConfigEditorBoolean
        @SearchTag("safari")
        @SearchTag("trajectory")
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.safariTrajectoryThrownCapsuleMode", desc = "config.babyzombieaddons.option.safariTrajectoryThrownCapsuleMode.desc")
        @ConfigEditorDropdown
        @SearchTag("safari")
        @SearchTag("trajectory")
        public ThrownCapsuleMode thrownCapsuleMode = ThrownCapsuleMode.CURRENT;

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.safariTrajectoryTrackedCapsuleBox", desc = "config.babyzombieaddons.option.safariTrajectoryTrackedCapsuleBox.desc")
        @ConfigEditorBoolean
        @SearchTag("safari")
        @SearchTag("trajectory")
        public boolean trackedCapsuleBoxEnabled = true;

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.safariTrajectoryLineColor", desc = "config.babyzombieaddons.option.safariTrajectoryLineColor.desc")
        @ConfigEditorColour
        public ChromaColour lineColor = ChromaColour.fromStaticRGB(80, 255, 220, 255);

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.safariTrajectoryLineThickness", desc = "config.babyzombieaddons.option.safariTrajectoryLineThickness.desc")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.1f)
        public float lineThickness = 0.6f;

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.safariTrajectoryLandingCube", desc = "config.babyzombieaddons.option.safariTrajectoryLandingCube.desc")
        @ConfigEditorBoolean
        public boolean landingCubeEnabled = true;

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.safariTrajectoryCubeColor", desc = "config.babyzombieaddons.option.safariTrajectoryCubeColor.desc")
        @ConfigEditorColour
        public ChromaColour landingColor = ChromaColour.fromStaticRGB(80, 255, 120, 255);

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.safariTrajectoryCubeSize", desc = "config.babyzombieaddons.option.safariTrajectoryCubeSize.desc")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.1f)
        public float landingSize = 0.5f;

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.safariTrajectoryLandingDisc", desc = "config.babyzombieaddons.option.safariTrajectoryLandingDisc.desc")
        @ConfigEditorBoolean
        public boolean landingDiscEnabled = true;

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.safariTrajectoryDiscColor", desc = "config.babyzombieaddons.option.safariTrajectoryDiscColor.desc")
        @ConfigEditorColour
        public ChromaColour landingDiscColor = ChromaColour.fromStaticRGB(80, 255, 120, 255);

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.safariTrajectoryDiscSize", desc = "config.babyzombieaddons.option.safariTrajectoryDiscSize.desc")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.1f)
        public float landingDiscSize = 0.5f;

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.safariTrajectoryBlockHighlight", desc = "config.babyzombieaddons.option.safariTrajectoryBlockHighlight.desc")
        @ConfigEditorBoolean
        public boolean blockHighlightEnabled = true;

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.safariTrajectoryBlockColor", desc = "config.babyzombieaddons.option.safariTrajectoryBlockColor.desc")
        @ConfigEditorColour
        public ChromaColour blockHighlightColor = ChromaColour.fromStaticRGB(255, 220, 60, 255);

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.safariTrajectoryBlockLineWidth", desc = "config.babyzombieaddons.option.safariTrajectoryBlockLineWidth.desc")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.1f)
        public float blockHighlightThickness = 0.5f;
    }

    /** 雪地群系（Icy）目标生物 */
    public static class Icy {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyTropicalFishGlow", desc = "config.babyzombieaddons.option.safariIcyTropicalFishGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("tropicalfish") @SearchTag("icy")
        public boolean tropicalFishGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyTropicalFishGlowColor", desc = "config.babyzombieaddons.option.safariIcyTropicalFishGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("tropicalfish") @SearchTag("icy")
        public ChromaColour tropicalFishGlowColor = ChromaColour.fromStaticRGB(255, 255, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyDolphinGlow", desc = "config.babyzombieaddons.option.safariIcyDolphinGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("dolphin") @SearchTag("icy")
        public boolean dolphinGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyDolphinGlowColor", desc = "config.babyzombieaddons.option.safariIcyDolphinGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("dolphin") @SearchTag("icy")
        public ChromaColour dolphinGlowColor = ChromaColour.fromStaticRGB(85, 85, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyGlowSquidGlow", desc = "config.babyzombieaddons.option.safariIcyGlowSquidGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("glowsquid") @SearchTag("icy")
        public boolean glowSquidGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyGlowSquidGlowColor", desc = "config.babyzombieaddons.option.safariIcyGlowSquidGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("glowsquid") @SearchTag("icy")
        public ChromaColour glowSquidGlowColor = ChromaColour.fromStaticRGB(85, 255, 85, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyPolarBearGlow", desc = "config.babyzombieaddons.option.safariIcyPolarBearGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("polarbear") @SearchTag("icy")
        public boolean polarBearGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyPolarBearGlowColor", desc = "config.babyzombieaddons.option.safariIcyPolarBearGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("polarbear") @SearchTag("icy")
        public ChromaColour polarBearGlowColor = ChromaColour.fromStaticRGB(85, 255, 85, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcySnowGolemGlow", desc = "config.babyzombieaddons.option.safariIcySnowGolemGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("snowgolem") @SearchTag("icy")
        public boolean snowGolemGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcySnowGolemGlowColor", desc = "config.babyzombieaddons.option.safariIcySnowGolemGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("snowgolem") @SearchTag("icy")
        public ChromaColour snowGolemGlowColor = ChromaColour.fromStaticRGB(255, 255, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyGoatGlow", desc = "config.babyzombieaddons.option.safariIcyGoatGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("goat") @SearchTag("icy")
        public boolean goatGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyGoatGlowColor", desc = "config.babyzombieaddons.option.safariIcyGoatGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("goat") @SearchTag("icy")
        public ChromaColour goatGlowColor = ChromaColour.fromStaticRGB(85, 85, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyRavagerGlow", desc = "config.babyzombieaddons.option.safariIcyRavagerGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("ravager") @SearchTag("icy")
        public boolean ravagerGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyRavagerGlowColor", desc = "config.babyzombieaddons.option.safariIcyRavagerGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("ravager") @SearchTag("icy")
        public ChromaColour ravagerGlowColor = ChromaColour.fromStaticRGB(255, 170, 0, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyMantisShrimpGlow", desc = "config.babyzombieaddons.option.safariIcyMantisShrimpGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("mantisshrimp") @SearchTag("icy")
        public boolean mantisShrimpGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyMantisShrimpGlowColor", desc = "config.babyzombieaddons.option.safariIcyMantisShrimpGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("mantisshrimp") @SearchTag("icy")
        public ChromaColour mantisShrimpGlowColor = ChromaColour.fromStaticRGB(85, 85, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyTroodonGlow", desc = "config.babyzombieaddons.option.safariIcyTroodonGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("troodon") @SearchTag("icy")
        public boolean troodonGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariIcyTroodonGlowColor", desc = "config.babyzombieaddons.option.safariIcyTroodonGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("troodon") @SearchTag("icy")
        public ChromaColour troodonGlowColor = ChromaColour.fromStaticRGB(85, 85, 255, 255);
    }

    /** Haunted 群系目标生物（含已有的 hideonwall/hideyho/蝙蝠/书架/warden/较频幽匿感测体） */
    public static class Haunted {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedHideonwallGlow", desc = "config.babyzombieaddons.option.safariHauntedHideonwallGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("hideonwall") @SearchTag("shulker") @SearchTag("haunted")
        public boolean hideonwallGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedHideonwallGlowColor", desc = "config.babyzombieaddons.option.safariHauntedHideonwallGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("hideonwall") @SearchTag("shulker") @SearchTag("haunted")
        public ChromaColour hideonwallGlowColor = ChromaColour.fromStaticRGB(85, 85, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedHideyhoGlow", desc = "config.babyzombieaddons.option.safariHauntedHideyhoGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("hideyho") @SearchTag("haunted")
        public boolean hideyhoGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedHideyhoGlowColor", desc = "config.babyzombieaddons.option.safariHauntedHideyhoGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("hideyho") @SearchTag("haunted")
        public ChromaColour hideyhoGlowColor = ChromaColour.fromStaticRGB(85, 85, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedBatGlow", desc = "config.babyzombieaddons.option.safariHauntedBatGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("bat") @SearchTag("haunted")
        public boolean batGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedBatGlowColor", desc = "config.babyzombieaddons.option.safariHauntedBatGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("bat") @SearchTag("haunted")
        public ChromaColour batGlowColor = ChromaColour.fromStaticRGB(85, 255, 85, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedDuplicoGlow", desc = "config.babyzombieaddons.option.safariHauntedDuplicoGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("duplico") @SearchTag("bookshelf") @SearchTag("haunted")
        public boolean duplicoGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedDuplicoGlowColor", desc = "config.babyzombieaddons.option.safariHauntedDuplicoGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("duplico") @SearchTag("bookshelf") @SearchTag("haunted")
        public ChromaColour duplicoGlowColor = ChromaColour.fromStaticRGB(85, 255, 85, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedWardenGlow", desc = "config.babyzombieaddons.option.safariHauntedWardenGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("warden") @SearchTag("haunted")
        public boolean wardenGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedWardenCooldownTicks", desc = "config.babyzombieaddons.option.safariHauntedWardenCooldownTicks.desc") @ConfigEditorSlider(minValue = 50, maxValue = 300, minStep = 1) @SearchTag("safari") @SearchTag("warden") @SearchTag("haunted")
        public int wardenCooldownTicks = 140;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedWardenGlowCooldownColor", desc = "config.babyzombieaddons.option.safariHauntedWardenGlowCooldownColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("warden") @SearchTag("haunted")
        public ChromaColour wardenGlowCooldownColor = ChromaColour.fromStaticRGB(255, 80, 80, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedWardenGlowReadyColor", desc = "config.babyzombieaddons.option.safariHauntedWardenGlowReadyColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("warden") @SearchTag("haunted")
        public ChromaColour wardenGlowReadyColor = ChromaColour.fromStaticRGB(80, 255, 80, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedSculkSensorGlow", desc = "config.babyzombieaddons.option.safariHauntedSculkSensorGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("sculksensor") @SearchTag("warden") @SearchTag("haunted")
        public boolean sculkSensorGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedSculkSensorGlowColor", desc = "config.babyzombieaddons.option.safariHauntedSculkSensorGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("sculksensor") @SearchTag("warden") @SearchTag("haunted")
        public ChromaColour sculkSensorGlowColor = ChromaColour.fromStaticRGB(170, 0, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedEndermiteGlow", desc = "config.babyzombieaddons.option.safariHauntedEndermiteGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("endermite") @SearchTag("haunted")
        public boolean endermiteGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedEndermiteGlowColor", desc = "config.babyzombieaddons.option.safariHauntedEndermiteGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("endermite") @SearchTag("haunted")
        public ChromaColour endermiteGlowColor = ChromaColour.fromStaticRGB(85, 255, 85, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedCaveSpiderGlow", desc = "config.babyzombieaddons.option.safariHauntedCaveSpiderGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("cavespider") @SearchTag("haunted")
        public boolean caveSpiderGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedCaveSpiderGlowColor", desc = "config.babyzombieaddons.option.safariHauntedCaveSpiderGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("cavespider") @SearchTag("haunted")
        public ChromaColour caveSpiderGlowColor = ChromaColour.fromStaticRGB(85, 255, 85, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedPhantomGlow", desc = "config.babyzombieaddons.option.safariHauntedPhantomGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("phantom") @SearchTag("haunted")
        public boolean phantomGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedPhantomGlowColor", desc = "config.babyzombieaddons.option.safariHauntedPhantomGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("phantom") @SearchTag("haunted")
        public ChromaColour phantomGlowColor = ChromaColour.fromStaticRGB(85, 255, 85, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedGimmiegoldGlow", desc = "config.babyzombieaddons.option.safariHauntedGimmiegoldGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("gimmiegold") @SearchTag("haunted")
        public boolean gimmiegoldGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariHauntedGimmiegoldGlowColor", desc = "config.babyzombieaddons.option.safariHauntedGimmiegoldGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("gimmiegold") @SearchTag("haunted")
        public ChromaColour gimmiegoldGlowColor = ChromaColour.fromStaticRGB(85, 85, 255, 255);
    }

    /** 洞穴群系（Cavern）目标生物 */
    public static class Cavern {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariCavernTropicalFishGlow", desc = "config.babyzombieaddons.option.safariCavernTropicalFishGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("tropicalfish") @SearchTag("cavern")
        public boolean tropicalFishGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariCavernTropicalFishGlowColor", desc = "config.babyzombieaddons.option.safariCavernTropicalFishGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("tropicalfish") @SearchTag("cavern")
        public ChromaColour tropicalFishGlowColor = ChromaColour.fromStaticRGB(255, 255, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariCavernArmadilloGlow", desc = "config.babyzombieaddons.option.safariCavernArmadilloGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("armadillo") @SearchTag("cavern")
        public boolean armadilloGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariCavernArmadilloGlowColor", desc = "config.babyzombieaddons.option.safariCavernArmadilloGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("armadillo") @SearchTag("cavern")
        public ChromaColour armadilloGlowColor = ChromaColour.fromStaticRGB(85, 85, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariCavernSnifferGlow", desc = "config.babyzombieaddons.option.safariCavernSnifferGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("sniffer") @SearchTag("cavern")
        public boolean snifferGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariCavernSnifferGlowColor", desc = "config.babyzombieaddons.option.safariCavernSnifferGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("sniffer") @SearchTag("cavern")
        public ChromaColour snifferGlowColor = ChromaColour.fromStaticRGB(85, 85, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariCavernSilverfishGlow", desc = "config.babyzombieaddons.option.safariCavernSilverfishGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("silverfish") @SearchTag("cavern")
        public boolean silverfishGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariCavernSilverfishGlowColor", desc = "config.babyzombieaddons.option.safariCavernSilverfishGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("silverfish") @SearchTag("cavern")
        public ChromaColour silverfishGlowColor = ChromaColour.fromStaticRGB(85, 85, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariCavernVexGlow", desc = "config.babyzombieaddons.option.safariCavernVexGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("vex") @SearchTag("cavern")
        public boolean vexGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariCavernVexGlowColor", desc = "config.babyzombieaddons.option.safariCavernVexGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("vex") @SearchTag("cavern")
        public ChromaColour vexGlowColor = ChromaColour.fromStaticRGB(170, 0, 170, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariCavernFlitterGlow", desc = "config.babyzombieaddons.option.safariCavernFlitterGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("flitter") @SearchTag("cavern")
        public boolean flitterGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariCavernFlitterGlowColor", desc = "config.babyzombieaddons.option.safariCavernFlitterGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("flitter") @SearchTag("cavern")
        public ChromaColour flitterGlowColor = ChromaColour.fromStaticRGB(255, 255, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariCavernChuckwallaGlow", desc = "config.babyzombieaddons.option.safariCavernChuckwallaGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("chuckwalla") @SearchTag("cavern")
        public boolean chuckwallaGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariCavernChuckwallaGlowColor", desc = "config.babyzombieaddons.option.safariCavernChuckwallaGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("chuckwalla") @SearchTag("cavern")
        public ChromaColour chuckwallaGlowColor = ChromaColour.fromStaticRGB(85, 85, 255, 255);
    }

    /** 森林群系（Forest）目标生物（含已有的 hideonfloor） */
    public static class Forest {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariForestHideonfloorGlow", desc = "config.babyzombieaddons.option.safariForestHideonfloorGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("hideonfloor") @SearchTag("shulker") @SearchTag("forest")
        public boolean hideonfloorGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariForestHideonfloorGlowColor", desc = "config.babyzombieaddons.option.safariForestHideonfloorGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("hideonfloor") @SearchTag("shulker") @SearchTag("forest")
        public ChromaColour hideonfloorGlowColor = ChromaColour.fromStaticRGB(85, 85, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariForestFoxGlow", desc = "config.babyzombieaddons.option.safariForestFoxGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("fox") @SearchTag("forest")
        public boolean foxGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariForestFoxGlowColor", desc = "config.babyzombieaddons.option.safariForestFoxGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("fox") @SearchTag("forest")
        public ChromaColour foxGlowColor = ChromaColour.fromStaticRGB(255, 255, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariForestPandaGlow", desc = "config.babyzombieaddons.option.safariForestPandaGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("panda") @SearchTag("forest")
        public boolean pandaGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariForestPandaGlowColor", desc = "config.babyzombieaddons.option.safariForestPandaGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("panda") @SearchTag("forest")
        public ChromaColour pandaGlowColor = ChromaColour.fromStaticRGB(85, 85, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariForestCreakingGlow", desc = "config.babyzombieaddons.option.safariForestCreakingGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("creaking") @SearchTag("forest")
        public boolean creakingGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariForestCreakingGlowColor", desc = "config.babyzombieaddons.option.safariForestCreakingGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("creaking") @SearchTag("forest")
        public ChromaColour creakingGlowColor = ChromaColour.fromStaticRGB(85, 255, 85, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariForestFrogGlow", desc = "config.babyzombieaddons.option.safariForestFrogGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("frog") @SearchTag("forest")
        public boolean frogGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariForestFrogGlowColor", desc = "config.babyzombieaddons.option.safariForestFrogGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("frog") @SearchTag("forest")
        public ChromaColour frogGlowColor = ChromaColour.fromStaticRGB(85, 255, 85, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariForestParrotGlow", desc = "config.babyzombieaddons.option.safariForestParrotGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("parrot") @SearchTag("forest")
        public boolean parrotGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariForestParrotGlowColor", desc = "config.babyzombieaddons.option.safariForestParrotGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("parrot") @SearchTag("forest")
        public ChromaColour parrotGlowColor = ChromaColour.fromStaticRGB(85, 85, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariForestBeeGlow", desc = "config.babyzombieaddons.option.safariForestBeeGlow.desc") @ConfigEditorBoolean @SearchTag("safari") @SearchTag("bee") @SearchTag("forest")
        public boolean beeGlow = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.safariForestBeeGlowColor", desc = "config.babyzombieaddons.option.safariForestBeeGlowColor.desc") @ConfigEditorColour @SearchTag("safari") @SearchTag("bee") @SearchTag("forest")
        public ChromaColour beeGlowColor = ChromaColour.fromStaticRGB(85, 255, 85, 255);
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
