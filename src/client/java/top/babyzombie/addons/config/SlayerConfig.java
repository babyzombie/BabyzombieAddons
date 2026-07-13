package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import top.babyzombie.addons.config.ModConfig.*;

public class SlayerConfig {

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.bossRespawnAlert", desc = "config.babyzombieaddons.option.bossRespawnAlert.desc") @ConfigEditorBoolean
    public boolean bossRespawnAlert = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.noslayerquest", desc = "config.babyzombieaddons.option.noslayerquest.desc") @ConfigEditorBoolean @SearchTag("auto")
    public boolean noslayerquest = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.boxLowHPBloodfiend", desc = "config.babyzombieaddons.option.boxLowHPBloodfiend.desc") @ConfigEditorBoolean @SearchTag("bloodfiend")
    public boolean boxLowHPBloodfiend = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.showEffigies", desc = "config.babyzombieaddons.option.showEffigies.desc") @ConfigEditorBoolean @SearchTag("effigy")
    public boolean showEffigies = false;

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.itemSkillTimers", desc = "") @Accordion
    public ItemSkillTimers itemSkillTimers = new ItemSkillTimers();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.slayerBossInfo", desc = "") @Accordion
    public SlayerBossInfo slayerBossInfo = new SlayerBossInfo();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.slayerBossBox", desc = "") @Accordion
    public SlayerBossBox slayerBossBox = new SlayerBossBox();

    public static class ItemSkillTimers {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pigmanSwordTimer", desc = "config.babyzombieaddons.option.pigmanSwordTimer.desc") @ConfigEditorBoolean @SearchTag("pigman")
        public boolean pigmanSwordTimer = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.holyIceTimer", desc = "config.babyzombieaddons.option.holyIceTimer.desc") @ConfigEditorBoolean @SearchTag("ice")
        public boolean holyIceTimer = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ragnarockAxeTimer", desc = "config.babyzombieaddons.option.ragnarockAxeTimer.desc") @ConfigEditorDropdown @SearchTag("ragnarock")
        public RagnarockAxeMode ragnarockAxeTimer = RagnarockAxeMode.OFF;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.reaperArmorTimer", desc = "config.babyzombieaddons.option.reaperArmorTimer.desc") @ConfigEditorBoolean @SearchTag("reaper")
        public boolean reaperArmorTimer = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.endStoneSwordTimer", desc = "config.babyzombieaddons.option.endStoneSwordTimer.desc") @ConfigEditorDropdown @SearchTag("endstone")
        public EndStoneSwordMode endStoneSwordTimer = EndStoneSwordMode.OFF;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.reheatedGummyPolarBear", desc = "config.babyzombieaddons.option.reheatedGummyPolarBear.desc") @ConfigEditorDropdown @SearchTag("gummy")
        public GummyPolarBearMode reheatedGummyPolarBear = GummyPolarBearMode.OFF;
    }

    public static class SlayerBossInfo {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.zombieSlayerInfo", desc = "config.babyzombieaddons.option.zombieSlayerInfo.desc") @ConfigEditorDropdown @SearchTag("zombie")
        public SlayerBossInfoMode zombieSlayerInfo = SlayerBossInfoMode.OFF;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.spiderSlayerInfo", desc = "config.babyzombieaddons.option.spiderSlayerInfo.desc") @ConfigEditorDropdown @SearchTag("spider")
        public SlayerBossInfoMode spiderSlayerInfo = SlayerBossInfoMode.OFF;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.wolfSlayerInfo", desc = "config.babyzombieaddons.option.wolfSlayerInfo.desc") @ConfigEditorDropdown @SearchTag("wolf")
        public SlayerBossInfoMode wolfSlayerInfo = SlayerBossInfoMode.OFF;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.endermanSlayerInfo", desc = "config.babyzombieaddons.option.endermanSlayerInfo.desc") @ConfigEditorDropdown @SearchTag("enderman")
        public SlayerBossInfoMode endermanSlayerInfo = SlayerBossInfoMode.OFF;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.blazeSlayerInfo", desc = "config.babyzombieaddons.option.blazeSlayerInfo.desc") @ConfigEditorDropdown @SearchTag("blaze")
        public SlayerBossInfoMode blazeSlayerInfo = SlayerBossInfoMode.OFF;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.vampireSlayerInfo", desc = "config.babyzombieaddons.option.vampireSlayerInfo.desc") @ConfigEditorDropdown @SearchTag("vampire")
        public SlayerBossInfoMode vampireSlayerInfo = SlayerBossInfoMode.OFF;
    }

    public static class SlayerBossBox {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.boxSlayerBoss", desc = "config.babyzombieaddons.option.boxSlayerBoss.desc") @ConfigEditorDropdown @SearchTag("box")
        public SlayerBossBoxMode boxSlayerBoss = SlayerBossBoxMode.OFF;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.boxBossRenderThroughWalls", desc = "config.babyzombieaddons.option.boxBossRenderThroughWalls.desc") @ConfigEditorBoolean
        public boolean boxBossRenderThroughWalls = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.boxBossLineWidth", desc = "config.babyzombieaddons.option.boxBossLineWidth.desc") @ConfigEditorSlider(minValue = 1, maxValue = 16, minStep = 1)
        public int boxBossLineWidth = 5;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.boxBossColor", desc = "config.babyzombieaddons.option.boxBossColor.desc") @ConfigEditorColour
        public ChromaColour boxBossColor = ChromaColour.fromStaticRGB(255, 255, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.boxBossBeam", desc = "config.babyzombieaddons.option.boxBossBeam.desc") @ConfigEditorBoolean
        public boolean boxBossBeam = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.boxBossBeamColor", desc = "config.babyzombieaddons.option.boxBossBeamColor.desc") @ConfigEditorColour
        public ChromaColour boxBossBeamColor = ChromaColour.fromStaticRGB(255, 255, 255, 255);
    }
}
