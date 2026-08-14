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
import top.babyzombie.addons.util.ChatUtils;

public class FishingConfig {

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.lavaBobberFix", desc = "config.babyzombieaddons.option.lavaBobberFix.desc") @ConfigEditorBoolean @SearchTag("lava") @SearchTag("bobber") @SearchTag("fishing")
    public boolean lavaBobberFix = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.group.rareSeaCreatures", desc = "")
    @Accordion
    public RareSeaCreatures rareSeaCreatures = new RareSeaCreatures();

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.group.preventInstantReel", desc = "")
    @Accordion
    public PreventInstantReel preventInstantReel = new PreventInstantReel();

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.group.fishingCamera", desc = "")
    @Accordion
    public FishingCamera fishingCamera = new FishingCamera();

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.popupBaitLow", desc = "config.babyzombieaddons.option.popupBaitLow.desc") @ConfigEditorSlider(minValue = 0, maxValue = 64, minStep = 1) @SearchTag("bait")
    public int popupBaitLow = 0;;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.group.muteVanquisher", desc = "")
    @Accordion
    public MuteVanquisher muteVanquisher = new MuteVanquisher();


    public static class RareSeaCreatures {
        @Expose
        @ConfigOption(name = "config.babyzombieaddons.group.rareSeaCreaturesSelfCaught", desc = "")
        @Accordion
        public RareSeaCreaturesSelfCaught selfCaught = new RareSeaCreaturesSelfCaught();

        public static class RareSeaCreaturesSelfCaught {
            @Expose
            @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesSelfCaught", desc = "config.babyzombieaddons.option.rareSeaCreaturesSelfCaught.desc")
            @ConfigEditorBoolean
            @SearchTag("rare")
            public boolean enabled = false;

            @Expose
            @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesSelfCaughtParty", desc = "config.babyzombieaddons.option.rareSeaCreaturesSelfCaughtParty.desc")
            @ConfigEditorBoolean
            @SearchTag("rare")
            public boolean partyChat = false;

            @Expose
            @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesSelfCaughtSound", desc = "config.babyzombieaddons.option.rareSeaCreaturesSelfCaughtSound.desc")
            @ConfigEditorBoolean
            @SearchTag("rare")
            public boolean playSound = false;

            @Expose
            @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesSelfCaughtTitle", desc = "config.babyzombieaddons.option.rareSeaCreaturesSelfCaughtTitle.desc")
            @ConfigEditorBoolean
            @SearchTag("rare")
            public boolean showTitle = false;
        }

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
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.flipflopper", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.flipflopper.desc") @ConfigEditorBoolean @SearchTag("flipflopper") @SearchTag("exclude")
            public boolean flipflopper = true;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.rareSeaCreaturesExclude.seashine", desc = "config.babyzombieaddons.option.rareSeaCreaturesExclude.seashine.desc") @ConfigEditorBoolean @SearchTag("seashine") @SearchTag("exclude")
            public boolean seashine = true;
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

    public static class FishingCamera {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingCamera", desc = "config.babyzombieaddons.option.fishingCamera.desc") @ConfigEditorBoolean @SearchTag("camera") @SearchTag("bobber")
        public boolean enabled = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingCameraOnlyLobbyOrSkyblock", desc = "config.babyzombieaddons.option.fishingCameraOnlyLobbyOrSkyblock.desc") @ConfigEditorBoolean @SearchTag("skyblock") @SearchTag("lobby")
        public boolean onlyLobbyOrSkyblock = true;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingCameraDisabledInKuudra", desc = "config.babyzombieaddons.option.fishingCameraDisabledInKuudra.desc") @ConfigEditorBoolean @SearchTag("kuudra")
        public boolean disabledInKuudra = true;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingCameraDisabledInDungeon", desc = "config.babyzombieaddons.option.fishingCameraDisabledInDungeon.desc") @ConfigEditorBoolean @SearchTag("dungeon")
        public boolean disabledInDungeon = true;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingCameraYawMode", desc = "config.babyzombieaddons.option.fishingCameraYawMode.desc") @ConfigEditorDropdown
        public CameraYawMode yawMode = CameraYawMode.FIXED;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingCameraPitch", desc = "config.babyzombieaddons.option.fishingCameraPitch.desc") @ConfigEditorSlider(minValue = 0, maxValue = 90, minStep = 1) @SearchTag("camera")
        public int pitch = 40;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingCameraYawOffset", desc = "config.babyzombieaddons.option.fishingCameraYawOffset.desc") @ConfigEditorSlider(minValue = -180, maxValue = 180, minStep = 1) @SearchTag("camera")
        public int yawOffset = 0;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingCameraYawSpinSpeed", desc = "config.babyzombieaddons.option.fishingCameraYawSpinSpeed.desc") @ConfigEditorSlider(minValue = 0, maxValue = 60, minStep = 1) @SearchTag("camera")
        public int yawSpinSpeed = 0;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingCameraDistance", desc = "config.babyzombieaddons.option.fishingCameraDistance.desc") @ConfigEditorSlider(minValue = 0, maxValue = 5, minStep = 0.1f) @SearchTag("camera")
        public double distance = 2.0;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingCameraViewDistance", desc = "config.babyzombieaddons.option.fishingCameraViewDistance.desc") @ConfigEditorSlider(minValue = 4, maxValue = 128, minStep = 1) @SearchTag("camera")
        public int viewDistance = 32;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingCameraFrameRate", desc = "config.babyzombieaddons.option.fishingCameraFrameRate.desc") @ConfigEditorSlider(minValue = 5, maxValue = 60, minStep = 1) @SearchTag("camera")
        public int frameRate = 30;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingCameraLinger", desc = "config.babyzombieaddons.option.fishingCameraLinger.desc") @ConfigEditorSlider(minValue = 0, maxValue = 60, minStep = 1) @SearchTag("camera")
        public int lingerTicks = 0;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingCameraDisableWithShaders", desc = "config.babyzombieaddons.option.fishingCameraDisableWithShaders.desc") @ConfigEditorBoolean @SearchTag("shader") @SearchTag("iris")
        public boolean disableWithShaders = true;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingCameraAspectRatio", desc = "config.babyzombieaddons.option.fishingCameraAspectRatio.desc") @ConfigEditorDropdown
        public CameraAspectRatio aspectRatio = CameraAspectRatio.R2_1;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingCameraBorderColor", desc = "config.babyzombieaddons.option.fishingCameraBorderColor.desc") @ConfigEditorColour
        public ChromaColour borderColor = ChromaColour.fromStaticRGB(128, 128, 128, 255);
    }

    public enum CameraAspectRatio {
        R2_1(2, 1), R1_2(1, 2), R1_1(1, 1), R16_10(16, 10), R16_9(16, 9), R4_3(4, 3);
        public final int w;
        public final int h;
        CameraAspectRatio(int w, int h) {
            this.w = w;
            this.h = h;
        }
        @Override public String toString() { return ChatUtils.translate("config.babyzombieaddons.option.fishingCameraAspectRatio." + name()); }
    }

    public enum CameraYawMode {
        FIXED, FRONT, BACK, LEFT, RIGHT;
        @Override public String toString() { return ChatUtils.translate("config.babyzombieaddons.option.fishingCameraYawMode." + name()); }
    }

    public static class MuteVanquisher {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.muteVanquisher.spawn", desc = "config.babyzombieaddons.option.muteVanquisher.spawn.desc") @ConfigEditorSlider(minValue = 0, maxValue = 1, minStep = 0.01f) @SearchTag("mute") @SearchTag("vanquisher")
        public float spawn = 1;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.muteVanquisher.idle", desc = "config.babyzombieaddons.option.muteVanquisher.idle.desc") @ConfigEditorSlider(minValue = 0, maxValue = 1, minStep = 0.01f) @SearchTag("mute") @SearchTag("vanquisher")
        public float idle = 1;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.muteVanquisher.hurt", desc = "config.babyzombieaddons.option.muteVanquisher.hurt.desc") @ConfigEditorSlider(minValue = 0, maxValue = 1, minStep = 0.01f) @SearchTag("mute") @SearchTag("vanquisher")
        public float hurt = 1;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.muteVanquisher.shoot", desc = "config.babyzombieaddons.option.muteVanquisher.shoot.desc") @ConfigEditorSlider(minValue = 0, maxValue = 1, minStep = 0.01f) @SearchTag("mute") @SearchTag("vanquisher")
        public float shoot = 1;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.muteVanquisher.death", desc = "config.babyzombieaddons.option.muteVanquisher.death.desc") @ConfigEditorSlider(minValue = 0, maxValue = 1, minStep = 0.01f) @SearchTag("mute") @SearchTag("vanquisher")
        public float death = 1;
    }
}
