package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.ChromaColour;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;

public class GardenConfig {

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.pestDisplay", desc = "config.babyzombieaddons.option.pestDisplay.desc") @ConfigEditorBoolean @SearchTag("pest")
    public boolean pestDisplay = false;

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.xpOrbSoundRemoval", desc = "config.babyzombieaddons.option.xpOrbSoundRemoval.desc") @ConfigEditorSlider(minValue = 0, maxValue = 100, minStep = 1) @SearchTag("xp")
    public int xpOrbSoundRemoval = 100;

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.signAutoRotate", desc = "config.babyzombieaddons.option.signAutoRotate.desc") @ConfigEditorBoolean @SearchTag("sign") @SearchTag("rotate") @SearchTag("garden")
    public boolean signAutoRotate = false;

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.farmingToolSwingSuppression", desc = "config.babyzombieaddons.option.farmingToolSwingSuppression.desc") @ConfigEditorBoolean @SearchTag("farming") @SearchTag("swing") @SearchTag("tool") @SearchTag("garden")
    public boolean farmingToolSwingSuppression = false;

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.plotBorder", desc = "") @Accordion
    public PlotBorder plotBorder = new PlotBorder();

    public static class PlotBorder {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.plotBorderDisplay", desc = "config.babyzombieaddons.option.plotBorderDisplay.desc") @ConfigEditorBoolean @SearchTag("plot") @SearchTag("border") @SearchTag("garden")
        public boolean enabled = false;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.plotBorderColor", desc = "config.babyzombieaddons.option.plotBorderColor.desc") @SearchTag("plot") @SearchTag("border") @SearchTag("color")
        public ChromaColour color = ChromaColour.fromStaticRGB(52, 118, 250, 255);
    }

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.greenhouseProtection", desc = "") @Accordion
    public GreenhouseProtection greenhouseProtection = new GreenhouseProtection();

    public static class GreenhouseProtection {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.greenhouseProtection", desc = "config.babyzombieaddons.option.greenhouseProtection.desc") @ConfigEditorBoolean @SearchTag("greenhouse") @SearchTag("protection") @SearchTag("garden")
        public boolean enabled = false;

        // ═══ 原版作物 ═══
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghWheat", desc = "config.babyzombieaddons.option.ghWheat.desc") @ConfigEditorBoolean @SearchTag("wheat") @SearchTag("greenhouse")
        public boolean wheat = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghCarrot", desc = "config.babyzombieaddons.option.ghCarrot.desc") @ConfigEditorBoolean @SearchTag("carrot") @SearchTag("greenhouse")
        public boolean carrot = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghPotato", desc = "config.babyzombieaddons.option.ghPotato.desc") @ConfigEditorBoolean @SearchTag("potato") @SearchTag("greenhouse")
        public boolean potato = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghBeetroot", desc = "config.babyzombieaddons.option.ghBeetroot.desc") @ConfigEditorBoolean @SearchTag("beetroot") @SearchTag("greenhouse")
        public boolean beetroot = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghPumpkin", desc = "config.babyzombieaddons.option.ghPumpkin.desc") @ConfigEditorBoolean @SearchTag("pumpkin") @SearchTag("greenhouse")
        public boolean pumpkin = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghMelon", desc = "config.babyzombieaddons.option.ghMelon.desc") @ConfigEditorBoolean @SearchTag("melon") @SearchTag("greenhouse")
        public boolean melon = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghCactus", desc = "config.babyzombieaddons.option.ghCactus.desc") @ConfigEditorBoolean @SearchTag("cactus") @SearchTag("greenhouse")
        public boolean cactus = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghSugarCane", desc = "config.babyzombieaddons.option.ghSugarCane.desc") @ConfigEditorBoolean @SearchTag("sugar") @SearchTag("cane") @SearchTag("greenhouse")
        public boolean sugarCane = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghRoseBush", desc = "config.babyzombieaddons.option.ghRoseBush.desc") @ConfigEditorBoolean @SearchTag("rose") @SearchTag("bush") @SearchTag("wildrose") @SearchTag("greenhouse")
        public boolean roseBush = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghSunflower", desc = "config.babyzombieaddons.option.ghSunflower.desc") @ConfigEditorBoolean @SearchTag("sunflower") @SearchTag("greenhouse")
        public boolean sunflower = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghDeadBush", desc = "config.babyzombieaddons.option.ghDeadBush.desc") @ConfigEditorBoolean @SearchTag("dead") @SearchTag("bush") @SearchTag("greenhouse")
        public boolean deadBush = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghFire", desc = "config.babyzombieaddons.option.ghFire.desc") @ConfigEditorBoolean @SearchTag("fire") @SearchTag("greenhouse")
        public boolean fire = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghCoco", desc = "config.babyzombieaddons.option.ghCoco.desc") @ConfigEditorBoolean @SearchTag("coco") @SearchTag("greenhouse")
        public boolean coco = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghNetherWart", desc = "config.babyzombieaddons.option.ghNetherWart.desc") @ConfigEditorBoolean @SearchTag("nether") @SearchTag("wart") @SearchTag("greenhouse")
        public boolean netherWart = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghMushroom", desc = "config.babyzombieaddons.option.ghMushroom.desc") @ConfigEditorBoolean @SearchTag("mushroom") @SearchTag("greenhouse")
        public boolean mushroom = false;

        // ═══ 杂交作物 ═══
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghAllInAloe", desc = "config.babyzombieaddons.option.ghAllInAloe.desc") @ConfigEditorBoolean @SearchTag("aloe") @SearchTag("greenhouse")
        public boolean allInAloe = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghAshwreath", desc = "config.babyzombieaddons.option.ghAshwreath.desc") @ConfigEditorBoolean @SearchTag("ashwreath") @SearchTag("greenhouse")
        public boolean ashwreath = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghBlastberry", desc = "config.babyzombieaddons.option.ghBlastberry.desc") @ConfigEditorBoolean @SearchTag("blastberry") @SearchTag("greenhouse")
        public boolean blastberry = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghCheesebite", desc = "config.babyzombieaddons.option.ghCheesebite.desc") @ConfigEditorBoolean @SearchTag("cheesebite") @SearchTag("greenhouse")
        public boolean cheesebite = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghChloronite", desc = "config.babyzombieaddons.option.ghChloronite.desc") @ConfigEditorBoolean @SearchTag("chloronite") @SearchTag("greenhouse")
        public boolean chloronite = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghChocoberry", desc = "config.babyzombieaddons.option.ghChocoberry.desc") @ConfigEditorBoolean @SearchTag("chocoberry") @SearchTag("greenhouse")
        public boolean chocoberry = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghChoconut", desc = "config.babyzombieaddons.option.ghChoconut.desc") @ConfigEditorBoolean @SearchTag("choconut") @SearchTag("greenhouse")
        public boolean choconut = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghChorusFruit", desc = "config.babyzombieaddons.option.ghChorusFruit.desc") @ConfigEditorBoolean @SearchTag("chorus") @SearchTag("fruit") @SearchTag("greenhouse")
        public boolean chorusFruit = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghCindershade", desc = "config.babyzombieaddons.option.ghCindershade.desc") @ConfigEditorBoolean @SearchTag("cindershade") @SearchTag("greenhouse")
        public boolean cindershade = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghCoalroot", desc = "config.babyzombieaddons.option.ghCoalroot.desc") @ConfigEditorBoolean @SearchTag("coalroot") @SearchTag("greenhouse")
        public boolean coalroot = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghCreambloom", desc = "config.babyzombieaddons.option.ghCreambloom.desc") @ConfigEditorBoolean @SearchTag("creambloom") @SearchTag("greenhouse")
        public boolean creambloom = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghDevourer", desc = "config.babyzombieaddons.option.ghDevourer.desc") @ConfigEditorBoolean @SearchTag("devourer") @SearchTag("greenhouse")
        public boolean devourer = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghDoNotEatShroom", desc = "config.babyzombieaddons.option.ghDoNotEatShroom.desc") @ConfigEditorBoolean @SearchTag("shroom") @SearchTag("greenhouse")
        public boolean doNotEatShroom = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghDuskbloom", desc = "config.babyzombieaddons.option.ghDuskbloom.desc") @ConfigEditorBoolean @SearchTag("duskbloom") @SearchTag("greenhouse")
        public boolean duskbloom = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghDustgrain", desc = "config.babyzombieaddons.option.ghDustgrain.desc") @ConfigEditorBoolean @SearchTag("dustgrain") @SearchTag("greenhouse")
        public boolean dustgrain = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghFleshtrap", desc = "config.babyzombieaddons.option.ghFleshtrap.desc") @ConfigEditorBoolean @SearchTag("fleshtrap") @SearchTag("greenhouse")
        public boolean fleshtrap = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghGlasscorn", desc = "config.babyzombieaddons.option.ghGlasscorn.desc") @ConfigEditorBoolean @SearchTag("glasscorn") @SearchTag("greenhouse")
        public boolean glasscorn = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghGloomgourd", desc = "config.babyzombieaddons.option.ghGloomgourd.desc") @ConfigEditorBoolean @SearchTag("gloomgourd") @SearchTag("greenhouse")
        public boolean gloomgourd = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghGodseed", desc = "config.babyzombieaddons.option.ghGodseed.desc") @ConfigEditorBoolean @SearchTag("godseed") @SearchTag("greenhouse")
        public boolean godseed = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghLonelily", desc = "config.babyzombieaddons.option.ghLonelily.desc") @ConfigEditorBoolean @SearchTag("lonelily") @SearchTag("greenhouse")
        public boolean lonelily = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghMagicJellybean", desc = "config.babyzombieaddons.option.ghMagicJellybean.desc") @ConfigEditorBoolean @SearchTag("jellybean") @SearchTag("greenhouse")
        public boolean magicJellybean = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghNoctilume", desc = "config.babyzombieaddons.option.ghNoctilume.desc") @ConfigEditorBoolean @SearchTag("noctilume") @SearchTag("greenhouse")
        public boolean noctilume = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghPhantomleaf", desc = "config.babyzombieaddons.option.ghPhantomleaf.desc") @ConfigEditorBoolean @SearchTag("phantomleaf") @SearchTag("greenhouse")
        public boolean phantomleaf = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghPlantboyAdvance", desc = "config.babyzombieaddons.option.ghPlantboyAdvance.desc") @ConfigEditorBoolean @SearchTag("plantboy") @SearchTag("greenhouse")
        public boolean plantboyAdvance = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghPuffercloud", desc = "config.babyzombieaddons.option.ghPuffercloud.desc") @ConfigEditorBoolean @SearchTag("puffercloud") @SearchTag("greenhouse")
        public boolean puffercloud = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghScourroot", desc = "config.babyzombieaddons.option.ghScourroot.desc") @ConfigEditorBoolean @SearchTag("scourroot") @SearchTag("greenhouse")
        public boolean scourroot = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghShadevine", desc = "config.babyzombieaddons.option.ghShadevine.desc") @ConfigEditorBoolean @SearchTag("shadevine") @SearchTag("greenhouse")
        public boolean shadevine = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghShellfruit", desc = "config.babyzombieaddons.option.ghShellfruit.desc") @ConfigEditorBoolean @SearchTag("shellfruit") @SearchTag("greenhouse")
        public boolean shellfruit = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghSnoozling", desc = "config.babyzombieaddons.option.ghSnoozling.desc") @ConfigEditorBoolean @SearchTag("snoozling") @SearchTag("greenhouse")
        public boolean snoozling = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghSoggybud", desc = "config.babyzombieaddons.option.ghSoggybud.desc") @ConfigEditorBoolean @SearchTag("soggybud") @SearchTag("greenhouse")
        public boolean soggybud = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghStartlevine", desc = "config.babyzombieaddons.option.ghStartlevine.desc") @ConfigEditorBoolean @SearchTag("startlevine") @SearchTag("greenhouse")
        public boolean startlevine = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghStoplightPetal", desc = "config.babyzombieaddons.option.ghStoplightPetal.desc") @ConfigEditorBoolean @SearchTag("stoplight") @SearchTag("petal") @SearchTag("greenhouse")
        public boolean stoplightPetal = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghThornshade", desc = "config.babyzombieaddons.option.ghThornshade.desc") @ConfigEditorBoolean @SearchTag("thornshade") @SearchTag("greenhouse")
        public boolean thornshade = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghThunderling", desc = "config.babyzombieaddons.option.ghThunderling.desc") @ConfigEditorBoolean @SearchTag("thunderling") @SearchTag("greenhouse")
        public boolean thunderling = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghTimestalk", desc = "config.babyzombieaddons.option.ghTimestalk.desc") @ConfigEditorBoolean @SearchTag("timestalk") @SearchTag("greenhouse")
        public boolean timestalk = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghTurtlellini", desc = "config.babyzombieaddons.option.ghTurtlellini.desc") @ConfigEditorBoolean @SearchTag("turtlellini") @SearchTag("greenhouse")
        public boolean turtlellini = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghVeilshroom", desc = "config.babyzombieaddons.option.ghVeilshroom.desc") @ConfigEditorBoolean @SearchTag("veilshroom") @SearchTag("greenhouse")
        public boolean veilshroom = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghWitherbloom", desc = "config.babyzombieaddons.option.ghWitherbloom.desc") @ConfigEditorBoolean @SearchTag("witherbloom") @SearchTag("greenhouse")
        public boolean witherbloom = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ghZombud", desc = "config.babyzombieaddons.option.ghZombud.desc") @ConfigEditorBoolean @SearchTag("zombud") @SearchTag("greenhouse")
        public boolean zombud = false;
    }

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.group.trevor", desc = "")
    @Accordion
    public Trevor trevor = new Trevor();

    public static class Trevor {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.trevorAutoAccept", desc = "config.babyzombieaddons.option.trevorAutoAccept.desc") @ConfigEditorBoolean @SearchTag("trevor")
        public boolean autoAccept = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.trevorAutoCall", desc = "config.babyzombieaddons.option.trevorAutoCall.desc") @ConfigEditorBoolean @SearchTag("trevor")
        public boolean autoCall = false;
    }
}
