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

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.popupBaitLow", desc = "config.babyzombieaddons.option.popupBaitLow.desc") @ConfigEditorSlider(minValue = 0, maxValue = 64, minStep = 1) @SearchTag("bait")
    public int popupBaitLow = 0;

    public static class RareSeaCreatures {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesAlert", desc = "config.babyzombieaddons.option.rareSeaCreaturesAlert.desc") @ConfigEditorBoolean @SearchTag("rare")
        public boolean alert = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesAlertTitle", desc = "config.babyzombieaddons.option.rareSeaCreaturesAlertTitle.desc") @ConfigEditorBoolean @SearchTag("rare")
        public boolean alertTitle = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesAlertTitleRepeat", desc = "config.babyzombieaddons.option.rareSeaCreaturesAlertTitleRepeat.desc") @ConfigEditorBoolean @SearchTag("rare")
        public boolean alertTitleRepeat = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesScanRange", desc = "config.babyzombieaddons.option.rareSeaCreaturesScanRange.desc") @ConfigEditorSlider(minValue = 1, maxValue = 25, minStep = 1) @SearchTag("rare")
        public int scanRange = 16;

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExcludeEnabled", desc = "config.babyzombieaddons.option.rareSeaCreaturesExcludeEnabled.desc")
        @ConfigEditorBoolean @SearchTag("exclude") @SearchTag("seacreature")
        public boolean excludeHighlightEnabled = false;

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.group.rareSeaCreaturesExcludeList", desc = "config.babyzombieaddons.group.rareSeaCreaturesExcludeList.desc")
        @Accordion
        public RareSeaCreaturesExcludeList excludeList = new RareSeaCreaturesExcludeList();
    }

    public static class RareSeaCreaturesExcludeList {
        @Expose
        @ConfigOption(name = "config.babyzombieaddons.group.rareSeaCreaturesExcludeWater", desc = "")
        @Accordion
        public WaterSeaCreatures waterSeaCreatures = new WaterSeaCreatures();

        @Expose
        @ConfigOption(name = "config.babyzombieaddons.group.rareSeaCreaturesExcludeLava", desc = "")
        @Accordion
        public LavaSeaCreatures lavaSeaCreatures = new LavaSeaCreatures();
    }

    public static class WaterSeaCreatures {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.waterHydra", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.waterHydra.desc") @ConfigEditorBoolean @SearchTag("hydra") @SearchTag("exclude")
        public boolean waterHydra = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.abyssalMiner", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.abyssalMiner.desc") @ConfigEditorBoolean @SearchTag("abyssal") @SearchTag("miner") @SearchTag("exclude")
        public boolean abyssalMiner = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.group.rareSeaCreaturesExclude.bayou", desc = "") @Accordion
        public Bayou bayou = new Bayou();
        @Expose @ConfigOption(name = "config.babyzombieaddons.group.rareSeaCreaturesExclude.galatea", desc = "") @Accordion
        public Galatea galatea = new Galatea();
        @Expose @ConfigOption(name = "config.babyzombieaddons.group.rareSeaCreaturesExclude.lotus", desc = "") @Accordion
        public Lotus lotus = new Lotus();
        @Expose @ConfigOption(name = "config.babyzombieaddons.group.rareSeaCreaturesExclude.torrhus", desc = "") @Accordion
        public Torrhus torrhus = new Torrhus();
        @Expose @ConfigOption(name = "config.babyzombieaddons.group.rareSeaCreaturesExclude.waterHotspot", desc = "") @Accordion
        public WaterHotspot waterHotspot = new WaterHotspot();
        @Expose @ConfigOption(name = "config.babyzombieaddons.group.rareSeaCreaturesExclude.jerrysWorkshop", desc = "") @Accordion
        public JerrysWorkshop jerrysWorkshop = new JerrysWorkshop();
        @Expose @ConfigOption(name = "config.babyzombieaddons.group.rareSeaCreaturesExclude.spookyFestival", desc = "") @Accordion
        public SpookyFestival spookyFestival = new SpookyFestival();
        @Expose @ConfigOption(name = "config.babyzombieaddons.group.rareSeaCreaturesExclude.fishingFestival", desc = "") @Accordion
        public FishingFestival fishingFestival = new FishingFestival();

        public static class Bayou {
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.alligator", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.alligator.desc") @ConfigEditorBoolean @SearchTag("alligator") @SearchTag("exclude")
            public boolean alligator = false;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.titanoboa", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.titanoboa.desc") @ConfigEditorBoolean @SearchTag("titanoboa") @SearchTag("exclude")
            public boolean titanoboa = false;
        }

        public static class Galatea {
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.theLochEmperor", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.theLochEmperor.desc") @ConfigEditorBoolean @SearchTag("loch") @SearchTag("emperor") @SearchTag("exclude")
            public boolean theLochEmperor = false;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.nessie", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.nessie.desc") @ConfigEditorBoolean @SearchTag("nessie") @SearchTag("exclude")
            public boolean nessie = false;
        }

        public static class Lotus {
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.puddleJumper", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.puddleJumper.desc") @ConfigEditorBoolean @SearchTag("puddle") @SearchTag("jumper") @SearchTag("exclude")
            public boolean puddleJumper = false;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.frogPrince", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.frogPrince.desc") @ConfigEditorBoolean @SearchTag("frog") @SearchTag("prince") @SearchTag("exclude")
            public boolean frogPrince = false;
        }

        public static class Torrhus {
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.silkbreeze", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.silkbreeze.desc") @ConfigEditorBoolean @SearchTag("silkbreeze") @SearchTag("exclude")
            public boolean silkbreeze = false;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.giantIsopod", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.giantIsopod.desc") @ConfigEditorBoolean @SearchTag("isopod") @SearchTag("exclude")
            public boolean giantIsopod = false;
        }

        public static class WaterHotspot {
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.blueRingedOctopus", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.blueRingedOctopus.desc") @ConfigEditorBoolean @SearchTag("octopus") @SearchTag("exclude")
            public boolean blueRingedOctopus = false;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.wikiTiki", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.wikiTiki.desc") @ConfigEditorBoolean @SearchTag("wiki") @SearchTag("tiki") @SearchTag("exclude")
            public boolean wikiTiki = false;
        }

        public static class JerrysWorkshop {
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.yeti", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.yeti.desc") @ConfigEditorBoolean @SearchTag("yeti") @SearchTag("exclude")
            public boolean yeti = false;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.reindrake", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.reindrake.desc") @ConfigEditorBoolean @SearchTag("reindrake") @SearchTag("exclude")
            public boolean reindrake = false;
        }

        public static class SpookyFestival {
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.phantomFisher", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.phantomFisher.desc") @ConfigEditorBoolean @SearchTag("phantom") @SearchTag("fisher") @SearchTag("exclude")
            public boolean phantomFisher = false;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.grimReaper", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.grimReaper.desc") @ConfigEditorBoolean @SearchTag("grim") @SearchTag("reaper") @SearchTag("exclude")
            public boolean grimReaper = false;
        }

        public static class FishingFestival {
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.greatWhiteShark", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.greatWhiteShark.desc") @ConfigEditorBoolean @SearchTag("shark") @SearchTag("exclude")
            public boolean greatWhiteShark = false;
        }
    }

    public static class LavaSeaCreatures {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.thunder", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.thunder.desc") @ConfigEditorBoolean @SearchTag("thunder") @SearchTag("exclude")
        public boolean thunder = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.lordJawbus", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.lordJawbus.desc") @ConfigEditorBoolean @SearchTag("jawbus") @SearchTag("exclude")
        public boolean lordJawbus = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.plhlegblast", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.plhlegblast.desc") @ConfigEditorBoolean @SearchTag("plhlegblast") @SearchTag("exclude")
        public boolean plhlegblast = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.group.rareSeaCreaturesExclude.lavaHotspot", desc = "") @Accordion
        public LavaHotspot lavaHotspot = new LavaHotspot();

        public static class LavaHotspot {
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.fieryScuttler", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.fieryScuttler.desc") @ConfigEditorBoolean @SearchTag("scuttler") @SearchTag("exclude")
            public boolean fieryScuttler = false;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.ragnarok", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.ragnarok.desc") @ConfigEditorBoolean @SearchTag("ragnarok") @SearchTag("exclude")
            public boolean ragnarok = false;
        }
    }

    public static class PreventInstantReel {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.preventInstantReel", desc = "config.babyzombieaddons.option.preventInstantReel.desc") @ConfigEditorBoolean @SearchTag("reel")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.preventInstantReelDelay", desc = "config.babyzombieaddons.option.preventInstantReelDelay.desc") @ConfigEditorSlider(minValue = 50, maxValue = 500, minStep = 1) @SearchTag("reel")
        public int delay = 200;
    }
}
