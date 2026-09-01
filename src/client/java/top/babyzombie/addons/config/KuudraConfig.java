package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import top.babyzombie.addons.config.ModConfig.*;
import top.babyzombie.addons.module.kuudra.ChestCounter;
import top.babyzombie.addons.module.kuudra.PearlWaypoints;

import java.util.ArrayList;
import java.util.List;

public class KuudraConfig {

    // ── Phase Accordions ──

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.kuudra_phase1", desc = "") @Accordion
    public Phase1 phase1 = new Phase1();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.kuudra_phase2", desc = "") @Accordion
    public Phase2 phase2 = new Phase2();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.kuudra_phase3", desc = "") @Accordion
    public Phase3 phase3 = new Phase3();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.kuudra_phase4", desc = "") @Accordion
    public Phase4 phase4 = new Phase4();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.requeue", desc = "") @Accordion
    public Requeue requeue = new Requeue();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.chestCounter", desc = "") @Accordion
    public ChestCounterCfg chestCounterCfg = new ChestCounterCfg();

    // ── General Kuudra ──

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.hpDisplay", desc = "config.babyzombieaddons.option.hpDisplay.desc") @ConfigEditorDropdown @SearchTag("hp") @SearchTag("health") @SearchTag("display")
    public HpDisplayMode hpDisplay = HpDisplayMode.OFF;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.phaseTimer", desc = "config.babyzombieaddons.option.phaseTimer.desc") @ConfigEditorBoolean @SearchTag("phase")
    public boolean phaseTimer = false;
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
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.etherwarpLavaPrevent", desc = "config.babyzombieaddons.option.etherwarpLavaPrevent.desc") @ConfigEditorBoolean @SearchTag("etherwarp") @SearchTag("lava")
    public boolean etherwarpLavaPrevent = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.fishingHookFix", desc = "config.babyzombieaddons.option.fishingHookFix.desc") @ConfigEditorBoolean @SearchTag("fishing") @SearchTag("bobber")
    public boolean fishingHookFix = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideMobNametags", desc = "config.babyzombieaddons.option.hideMobNametags.desc") @ConfigEditorBoolean @SearchTag("hide") @SearchTag("nametag") @SearchTag("mob")
    public boolean hideMobNametags = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.teamHighlight", desc = "config.babyzombieaddons.option.teamHighlight.desc") @ConfigEditorBoolean @SearchTag("team") @SearchTag("highlight") @SearchTag("glow")
    public boolean teamHighlight = false;
    @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.teamHighlightColor", desc = "config.babyzombieaddons.option.teamHighlightColor.desc") @SearchTag("team") @SearchTag("highlight") @SearchTag("color")
    public ChromaColour teamHighlightColor = ChromaColour.fromStaticRGB(0, 255, 0, 255);

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.kuudraMinimap", desc = "") @Accordion @SearchTag("minimap") @SearchTag("map")
    public MinimapCfg minimap = new MinimapCfg();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.perkShop", desc = "") @Accordion
    public PerkShop perkShop = new PerkShop();

    // ── Phase 1: Supplies ──

    public static class Phase1 {
        // 珍珠投掷路径点(置顶)
        @Expose @ConfigOption(name = "config.babyzombieaddons.group.pearlWaypoints", desc = "") @Accordion @SearchTag("pearl")
        public PearlWaypointsCfg pearlWaypoints = new PearlWaypointsCfg();
        // 珍珠轨迹(手持预测/投出追踪/第二相机)
        @Expose @ConfigOption(name = "config.babyzombieaddons.group.pearlTrajectory", desc = "") @Accordion @SearchTag("pearl")
        public PearlTrajectoryCfg pearlTrajectory = new PearlTrajectoryCfg();

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.supplyBeacons", desc = "config.babyzombieaddons.option.supplyBeacons.desc") @ConfigEditorBoolean @SearchTag("supply")
        public boolean supplyBeacons = false;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.supplyBeaconColor", desc = "config.babyzombieaddons.option.supplyBeaconColor.desc") @SearchTag("supply") @SearchTag("beacon") @SearchTag("color")
        public ChromaColour supplyBeaconColor = ChromaColour.fromStaticRGB(0, 255, 0, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.supplyPileWaypoints", desc = "config.babyzombieaddons.option.supplyPileWaypoints.desc") @ConfigEditorBoolean @SearchTag("supply") @SearchTag("pile")
        public boolean supplyPileWaypoints = false;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.supplyPileColor", desc = "config.babyzombieaddons.option.supplyPileColor.desc") @SearchTag("supply") @SearchTag("pile") @SearchTag("color")
        public ChromaColour supplyPileColor = ChromaColour.fromStaticRGB(255, 255, 255, 52);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.supplyPileNames", desc = "config.babyzombieaddons.option.supplyPileNames.desc") @ConfigEditorBoolean @SearchTag("supply") @SearchTag("pile") @SearchTag("name")
        public boolean supplyPileNames = false;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.supplyPileNameColor", desc = "config.babyzombieaddons.option.supplyPileNameColor.desc") @SearchTag("supply") @SearchTag("pile") @SearchTag("name") @SearchTag("color")
        public ChromaColour supplyPileNameColor = ChromaColour.fromStaticRGB(255, 255, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.supplyInteractionZone", desc = "config.babyzombieaddons.option.supplyInteractionZone.desc") @ConfigEditorBoolean @SearchTag("supply") @SearchTag("interaction")
        public boolean supplyInteractionZone = false;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.supplyZombieBoxColor", desc = "config.babyzombieaddons.option.supplyZombieBoxColor.desc") @SearchTag("supply") @SearchTag("zombie") @SearchTag("color")
        public ChromaColour supplyZombieBoxColor = ChromaColour.fromStaticRGB(0, 255, 0, 128);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.supplyPullCircle", desc = "config.babyzombieaddons.option.supplyPullCircle.desc") @ConfigEditorBoolean @SearchTag("supply") @SearchTag("circle")
        public boolean supplyPullCircle = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.supplyPlaceTimerChat", desc = "config.babyzombieaddons.option.supplyPlaceTimerChat.desc") @ConfigEditorBoolean @SearchTag("supply") @SearchTag("timer")
        public boolean supplyPlaceTimerChat = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.supplyPlaceTimerHud", desc = "config.babyzombieaddons.option.supplyPlaceTimerHud.desc") @ConfigEditorBoolean @SearchTag("supply") @SearchTag("timer") @SearchTag("hud")
        public boolean supplyPlaceTimerHud = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.supplyProgressHud", desc = "config.babyzombieaddons.option.supplyProgressHud.desc") @ConfigEditorBoolean @SearchTag("supply") @SearchTag("progress") @SearchTag("hud")
        public boolean supplyProgressHud = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.supplyGiantHitbox", desc = "config.babyzombieaddons.option.supplyGiantHitbox.desc") @ConfigEditorBoolean @SearchTag("supply") @SearchTag("giant")
        public boolean supplyGiantHitbox = false;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.supplyGiantHitboxColor", desc = "config.babyzombieaddons.option.supplyGiantHitboxColor.desc") @SearchTag("supply") @SearchTag("giant") @SearchTag("color")
        public ChromaColour supplyGiantHitboxColor = ChromaColour.fromStaticRGB(255, 255, 0, 128);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.noPreAlert", desc = "config.babyzombieaddons.option.noPreAlert.desc") @ConfigEditorBoolean @SearchTag("supply") @SearchTag("pre") @SearchTag("alert")
        public boolean noPreAlert = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.alreadyPickingAlert", desc = "config.babyzombieaddons.option.alreadyPickingAlert.desc") @ConfigEditorBoolean @SearchTag("supply") @SearchTag("alert")
        public boolean alreadyPickingAlert = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideTentacleTitle", desc = "config.babyzombieaddons.option.hideTentacleTitle.desc") @ConfigEditorBoolean @SearchTag("hide") @SearchTag("tentacle") @SearchTag("title")
        public boolean hideTentacleTitle = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.protectLocalScreenOnStart", desc = "config.babyzombieaddons.option.protectLocalScreenOnStart.desc") @ConfigEditorBoolean @SearchTag("screen") @SearchTag("close") @SearchTag("protect")
        public boolean protectLocalScreenOnStart = false;
    }

    // ── Phase 2: Build ──

    public static class Phase2 {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.freshMessage", desc = "config.babyzombieaddons.option.freshMessage.desc") @ConfigEditorBoolean @SearchTag("fresh")
        public boolean freshMessage = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.freshHighlight", desc = "config.babyzombieaddons.option.freshHighlight.desc") @ConfigEditorBoolean @SearchTag("fresh") @SearchTag("glow")
        public boolean freshHighlight = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.freshHistory", desc = "config.babyzombieaddons.option.freshHistory.desc") @ConfigEditorBoolean @SearchTag("fresh") @SearchTag("history")
        public boolean freshHistory = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.elleHighlight", desc = "config.babyzombieaddons.option.elleHighlight.desc") @ConfigEditorBoolean @SearchTag("elle") @SearchTag("highlight")
        public boolean elleHighlight = false;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.elleHighlightColor", desc = "config.babyzombieaddons.option.elleHighlightColor.desc") @SearchTag("elle") @SearchTag("color")
        public ChromaColour elleHighlightColor = ChromaColour.fromStaticRGB(255, 128, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.buildProgressHud", desc = "config.babyzombieaddons.option.buildProgressHud.desc") @ConfigEditorBoolean @SearchTag("build") @SearchTag("progress")
        public boolean buildProgressHud = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.buildStartCountdown", desc = "config.babyzombieaddons.option.buildStartCountdown.desc") @ConfigEditorBoolean @SearchTag("build") @SearchTag("countdown")
        public boolean buildStartCountdown = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ballistaProximityCircles", desc = "config.babyzombieaddons.option.ballistaProximityCircles.desc") @ConfigEditorBoolean @SearchTag("ballista") @SearchTag("circle")
        public boolean ballistaProximityCircles = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ballistaProgressText", desc = "config.babyzombieaddons.option.ballistaProgressText.desc") @ConfigEditorBoolean @SearchTag("ballista")
        public boolean ballistaProgressText = false;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.ballistaTextColor", desc = "config.babyzombieaddons.option.ballistaTextColor.desc") @SearchTag("ballista") @SearchTag("color")
        public ChromaColour ballistaTextColor = ChromaColour.fromStaticRGB(255, 255, 85, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ballistaBuildBeacons", desc = "config.babyzombieaddons.option.ballistaBuildBeacons.desc") @ConfigEditorBoolean @SearchTag("ballista")
        public boolean ballistaBuildBeacons = false;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.ballistaBeaconColor", desc = "config.babyzombieaddons.option.ballistaBeaconColor.desc") @SearchTag("ballista") @SearchTag("beacon") @SearchTag("color")
        public ChromaColour ballistaBeaconColor = ChromaColour.fromStaticRGB(255, 255, 0, 255);
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.ballistaInCircleColor", desc = "config.babyzombieaddons.option.ballistaInCircleColor.desc") @SearchTag("ballista") @SearchTag("beacon") @SearchTag("color")
        public ChromaColour ballistaInCircleColor = ChromaColour.fromStaticRGB(0, 255, 0, 255);
    }

    // ── Phase 3: Stun ──

    public static class Phase3 {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.stunTimer", desc = "config.babyzombieaddons.option.stunTimer.desc") @ConfigEditorBoolean @SearchTag("stun")
        public boolean stunTimer = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.energyDisplay", desc = "config.babyzombieaddons.option.energyDisplay.desc") @ConfigEditorBoolean @SearchTag("energy")
        public boolean energyDisplay = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fuelProgressHud", desc = "config.babyzombieaddons.option.fuelProgressHud.desc") @ConfigEditorBoolean @SearchTag("fuel") @SearchTag("progress") @SearchTag("hud")
        public boolean fuelProgressHud = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fuelOrbBeacons", desc = "config.babyzombieaddons.option.fuelOrbBeacons.desc") @ConfigEditorBoolean @SearchTag("fuel")
        public boolean fuelOrbBeacons = false;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.fuelOrbBeaconColor", desc = "config.babyzombieaddons.option.fuelOrbBeaconColor.desc") @SearchTag("fuel") @SearchTag("beacon") @SearchTag("color")
        public ChromaColour fuelOrbBeaconColor = ChromaColour.fromStaticRGB(255, 0, 0, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fuelInteractionZone", desc = "config.babyzombieaddons.option.fuelInteractionZone.desc") @ConfigEditorBoolean @SearchTag("fuel") @SearchTag("interaction")
        public boolean fuelInteractionZone = false;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.fuelZombieBoxColor", desc = "config.babyzombieaddons.option.fuelZombieBoxColor.desc") @SearchTag("fuel") @SearchTag("zombie") @SearchTag("color")
        public ChromaColour fuelZombieBoxColor = ChromaColour.fromStaticRGB(0, 255, 0, 128);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.fuelOrbPullCircle", desc = "config.babyzombieaddons.option.fuelOrbPullCircle.desc") @ConfigEditorBoolean @SearchTag("fuel") @SearchTag("circle")
        public boolean fuelOrbPullCircle = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.chuckBeacons", desc = "config.babyzombieaddons.option.chuckBeacons.desc") @ConfigEditorBoolean @SearchTag("chuck")
        public boolean chuckBeacons = false;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.chuckBeaconColor", desc = "config.babyzombieaddons.option.chuckBeaconColor.desc") @SearchTag("chuck") @SearchTag("beacon") @SearchTag("color")
        public ChromaColour chuckBeaconColor = ChromaColour.fromStaticRGB(255, 170, 0, 255);

        @Expose @ConfigOption(name = "config.babyzombieaddons.group.arrowPoison", desc = "") @Accordion
        public ArrowPoison arrowPoison = new ArrowPoison();
    }

    // ── Phase 4: Boss ──

    public static class Phase4 {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.directionHud", desc = "config.babyzombieaddons.option.directionHud.desc") @ConfigEditorBoolean @SearchTag("direction") @SearchTag("hud")
        public boolean directionHud = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraDistance", desc = "config.babyzombieaddons.option.kuudraDistance.desc") @ConfigEditorBoolean @SearchTag("distance") @SearchTag("hud")
        public boolean kuudraDistance = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.rendTracker", desc = "config.babyzombieaddons.option.rendTracker.desc") @ConfigEditorBoolean @SearchTag("rend") @SearchTag("damage")
        public boolean rendTracker = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.ichorPoolWaypoints", desc = "config.babyzombieaddons.option.ichorPoolWaypoints.desc") @ConfigEditorBoolean @SearchTag("ichor") @SearchTag("pool")
        public boolean ichorPoolWaypoints = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideKuudraDamageTitle", desc = "config.babyzombieaddons.option.hideKuudraDamageTitle.desc") @ConfigEditorBoolean @SearchTag("hide") @SearchTag("damage") @SearchTag("title")
        public boolean hideKuudraDamageTitle = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.p4ChuckBeacons", desc = "config.babyzombieaddons.option.p4ChuckBeacons.desc") @ConfigEditorBoolean @SearchTag("ball") @SearchTag("chuck") @SearchTag("beacon")
        public boolean p4ChuckBeacons = false;
    }

    // ── Shared inner classes (kept at top level for backward-compat reference via KuudraConfig.ArrowPoison) ──

    public static class ArrowPoison {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.toxicArrowMinTier", desc = "config.babyzombieaddons.option.toxicArrowMinTier.desc") @ConfigEditorDropdown @SearchTag("toxic") @SearchTag("arrow")
        public ToxicArrowMinTier toxicArrowMinTier = ToxicArrowMinTier.T3;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.toxicArrowTiming", desc = "config.babyzombieaddons.option.toxicArrowTiming.desc") @ConfigEditorDropdown @SearchTag("toxic") @SearchTag("arrow")
        public ToxicArrowTiming toxicArrowTiming = ToxicArrowTiming.KUUDRA_STUNNED;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.toxicArrowThreshold", desc = "config.babyzombieaddons.option.toxicArrowThreshold.desc") @ConfigEditorSlider(minValue = 0, maxValue = 64, minStep = 1) @SearchTag("toxic") @SearchTag("arrow") @SearchTag("threshold")
        public int toxicArrowThreshold = 0;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.toxicArrowPerMissing", desc = "config.babyzombieaddons.option.toxicArrowPerMissing.desc") @ConfigEditorSlider(minValue = 0, maxValue = 16, minStep = 1) @SearchTag("toxic") @SearchTag("arrow") @SearchTag("missing")
        public int toxicArrowPerMissing = 0;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.twilightArrowTiming", desc = "config.babyzombieaddons.option.twilightArrowTiming.desc") @ConfigEditorDropdown @SearchTag("twilight") @SearchTag("arrow")
        public TwilightArrowTiming twilightArrowTiming = TwilightArrowTiming.P4_START;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.twilightArrowThreshold", desc = "config.babyzombieaddons.option.twilightArrowThreshold.desc") @ConfigEditorSlider(minValue = 0, maxValue = 8, minStep = 1) @SearchTag("twilight") @SearchTag("arrow") @SearchTag("threshold")
        public int twilightArrowThreshold = 0;
    }

    public static class PerkShop {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.perkShopWhitelist", desc = "config.babyzombieaddons.option.perkShopWhitelist.desc") @ConfigEditorBoolean @SearchTag("perk")
        public boolean perkShopWhitelist = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.perkShopWhitelistItems", desc = "config.babyzombieaddons.option.perkShopWhitelistItems.desc") @ConfigEditorDraggableList @SearchTag("perk") @SearchTag("whitelist")
        public List<PerkShopItem> perkShopWhitelistItems = new ArrayList<>(List.of(
                PerkShopItem.SPECIALIST_ROUTE,
                PerkShopItem.BALLISTA_MECHANIC,
                PerkShopItem.HUMAN_CANNONBALL,
                PerkShopItem.REMOTE_PERK_SHOP,
                PerkShopItem.FILL_YOUR_QUIVER
        ));
    }

    public static class Requeue {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraRequeue", desc = "config.babyzombieaddons.option.kuudraRequeue.desc") @ConfigEditorDropdown @SearchTag("requeue")
        public RequeueMode kuudraRequeue = RequeueMode.OFF;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraRequeueDelay", desc = "config.babyzombieaddons.option.kuudraRequeueDelay.desc") @ConfigEditorSlider(minValue = 0, maxValue = 60, minStep = 1) @SearchTag("requeue") @SearchTag("delay")
        public int kuudraRequeueDelay = 0;
    }

    public static class PearlWaypointsCfg {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlWaypoints", desc = "config.babyzombieaddons.option.pearlWaypoints.desc") @ConfigEditorBoolean @SearchTag("pearl")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlShowBox", desc = "config.babyzombieaddons.option.pearlShowBox.desc") @ConfigEditorBoolean @SearchTag("pearl")
        public boolean showBox = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlShowTimer", desc = "config.babyzombieaddons.option.pearlShowTimer.desc") @ConfigEditorBoolean @SearchTag("pearl")
        public boolean showTimer = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlShowOutline", desc = "config.babyzombieaddons.option.pearlShowOutline.desc") @ConfigEditorBoolean @SearchTag("pearl")
        public boolean showOutline = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlThrowAlert", desc = "config.babyzombieaddons.option.pearlThrowAlert.desc") @ConfigEditorBoolean @SearchTag("pearl") @SearchTag("alert") @SearchTag("sound")
        public boolean throwAlert = false;
        @ConfigOption(name = "config.babyzombieaddons.option.pearlEditConfig", desc = "config.babyzombieaddons.option.pearlEditConfig.desc") @ConfigEditorButton(buttonText = "Edit")
        public transient Runnable editConfig = PearlWaypoints::openConfigFile;
        @ConfigOption(name = "config.babyzombieaddons.option.pearlOpenIqModrinth", desc = "config.babyzombieaddons.option.pearlOpenIqModrinth.desc") @ConfigEditorButton(buttonText = "IQ Addons")
        public transient Runnable openIqModrinth = PearlWaypoints::openIqModrinth;
        @ConfigOption(name = "config.babyzombieaddons.option.pearlOpenIqSource", desc = "config.babyzombieaddons.option.pearlOpenIqSource.desc") @ConfigEditorButton(buttonText = "IQ GitHub")
        public transient Runnable openIqSource = PearlWaypoints::openIqSource;
    }

    /** 珍珠轨迹(组中组):手持预测/投出追踪/第二相机。 */
    public static class PearlTrajectoryCfg {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlTrajectoryEnabled", desc = "config.babyzombieaddons.option.pearlTrajectoryEnabled.desc") @ConfigEditorBoolean @SearchTag("pearl") @SearchTag("trajectory")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlTrajectoryOnlyInKuudra", desc = "config.babyzombieaddons.option.pearlTrajectoryOnlyInKuudra.desc") @ConfigEditorBoolean @SearchTag("pearl") @SearchTag("kuudra")
        public boolean onlyInKuudra = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlTrajectoryOnlyPhase1", desc = "config.babyzombieaddons.option.pearlTrajectoryOnlyPhase1.desc") @ConfigEditorBoolean @SearchTag("pearl") @SearchTag("phase")
        public boolean onlyPhase1 = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlTrajectoryCamera", desc = "config.babyzombieaddons.option.pearlTrajectoryCamera.desc") @ConfigEditorBoolean @SearchTag("pearl") @SearchTag("camera")
        public boolean cameraEnabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlTrajectoryFollowPearl", desc = "config.babyzombieaddons.option.pearlTrajectoryFollowPearl.desc") @ConfigEditorBoolean @SearchTag("pearl") @SearchTag("follow")
        public boolean followPearl = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlTrajectoryLine", desc = "config.babyzombieaddons.option.pearlTrajectoryLine.desc") @ConfigEditorBoolean @SearchTag("pearl") @SearchTag("line")
        public boolean trajectoryEnabled = true;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.pearlTrajectoryLineColor", desc = "config.babyzombieaddons.option.pearlTrajectoryLineColor.desc") @SearchTag("pearl") @SearchTag("line") @SearchTag("color")
        public ChromaColour trajectoryColor = ChromaColour.fromStaticRGB(170, 0, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlTrajectoryDisc", desc = "config.babyzombieaddons.option.pearlTrajectoryDisc.desc") @ConfigEditorBoolean @SearchTag("pearl") @SearchTag("disc")
        public boolean landingDiscEnabled = true;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.pearlTrajectoryDiscColor", desc = "config.babyzombieaddons.option.pearlTrajectoryDiscColor.desc") @SearchTag("pearl") @SearchTag("disc") @SearchTag("color")
        public ChromaColour landingDiscColor = ChromaColour.fromStaticRGB(170, 0, 255, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlCameraYawMode", desc = "config.babyzombieaddons.option.pearlCameraYawMode.desc") @ConfigEditorDropdown @SearchTag("pearl")
        public FishingConfig.CameraYawMode pearlCameraYawMode = FishingConfig.CameraYawMode.FRONT;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlCameraAspectRatio", desc = "config.babyzombieaddons.option.pearlCameraAspectRatio.desc") @ConfigEditorDropdown @SearchTag("pearl")
        public FishingConfig.CameraAspectRatio pearlCameraAspectRatio = FishingConfig.CameraAspectRatio.R2_1;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlCameraPitch", desc = "config.babyzombieaddons.option.pearlCameraPitch.desc") @ConfigEditorSlider(minValue = 0, maxValue = 90, minStep = 1) @SearchTag("pearl")
        public int pearlCameraPitch = 40;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlCameraYawOffset", desc = "config.babyzombieaddons.option.pearlCameraYawOffset.desc") @ConfigEditorSlider(minValue = -180, maxValue = 180, minStep = 1) @SearchTag("pearl")
        public int pearlCameraYawOffset = 0;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlCameraYawSpinSpeed", desc = "config.babyzombieaddons.option.pearlCameraYawSpinSpeed.desc") @ConfigEditorSlider(minValue = 0, maxValue = 60, minStep = 1) @SearchTag("pearl")
        public int pearlCameraYawSpinSpeed = 0;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlCameraDistance", desc = "config.babyzombieaddons.option.pearlCameraDistance.desc") @ConfigEditorSlider(minValue = 0, maxValue = 5, minStep = 0.1f) @SearchTag("pearl")
        public double pearlCameraDistance = 3.0;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlCameraViewDistance", desc = "config.babyzombieaddons.option.pearlCameraViewDistance.desc") @ConfigEditorSlider(minValue = 16, maxValue = 128, minStep = 1) @SearchTag("pearl")
        public int pearlCameraViewDistance = 32;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlCameraFrameRate", desc = "config.babyzombieaddons.option.pearlCameraFrameRate.desc") @ConfigEditorSlider(minValue = 5, maxValue = 60, minStep = 1) @SearchTag("pearl")
        public int pearlCameraFrameRate = 30;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.pearlCameraBorderColor", desc = "config.babyzombieaddons.option.pearlCameraBorderColor.desc") @SearchTag("pearl")
        public ChromaColour pearlCameraBorderColor = ChromaColour.fromStaticRGB(128, 128, 128, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.pearlTrajectoryShowTime", desc = "config.babyzombieaddons.option.pearlTrajectoryShowTime.desc") @ConfigEditorBoolean @SearchTag("pearl") @SearchTag("time")
        public boolean showLandingTime = true;
    }

    public static class ChestCounterCfg {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.chestCounter", desc = "config.babyzombieaddons.option.chestCounter.desc") @ConfigEditorBoolean @SearchTag("chest")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.chestCounterMode", desc = "config.babyzombieaddons.option.chestCounterMode.desc") @ConfigEditorDropdown @SearchTag("chest")
        public ModConfig.ChestCounterMode displayMode = ChestCounterMode.INCLUDE_CRIMSON_DUNGEON    ;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.chestCounterInteract", desc = "config.babyzombieaddons.option.chestCounterInteract.desc") @ConfigEditorBoolean @SearchTag("chest")
        public boolean interact = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.chestCounterParty", desc = "config.babyzombieaddons.option.chestCounterParty.desc") @ConfigEditorBoolean @SearchTag("chest")
        public boolean partyAnnounce = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.chestCounterSound", desc = "config.babyzombieaddons.option.chestCounterSound.desc") @ConfigEditorBoolean @SearchTag("chest")
        public boolean sound = false;
        @ConfigOption(name = "config.babyzombieaddons.option.chestCounterReset", desc = "config.babyzombieaddons.option.chestCounterReset.desc") @ConfigEditorButton(buttonText = "Reset")
        public transient Runnable reset = ChestCounter::resetCounter;
    }

    /** Kuudra 小地图:固定全图俯视图 + 各类标记,元素开关独立可配。 */
    public static class MinimapCfg {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimap", desc = "config.babyzombieaddons.option.kuudraMinimap.desc") @ConfigEditorBoolean @SearchTag("minimap") @SearchTag("map") @SearchTag("kuudra")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapSize", desc = "config.babyzombieaddons.option.kuudraMinimapSize.desc") @ConfigEditorSlider(minValue = 96, maxValue = 320, minStep = 16) @SearchTag("minimap") @SearchTag("size")
        public int size = 160;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapTeammates", desc = "config.babyzombieaddons.option.kuudraMinimapTeammates.desc") @ConfigEditorBoolean @SearchTag("minimap") @SearchTag("teammate")
        public boolean teammates = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.teammateStyle", desc = "config.babyzombieaddons.option.teammateStyle.desc") @ConfigEditorDropdown @SearchTag("minimap") @SearchTag("teammate")
        public ModConfig.KuudraMinimapTeammateStyle teammateStyle = ModConfig.KuudraMinimapTeammateStyle.DOT_NAME;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapHeadRotate", desc = "config.babyzombieaddons.option.kuudraMinimapHeadRotate.desc") @ConfigEditorBoolean @SearchTag("minimap") @SearchTag("head") @SearchTag("rotate")
        public boolean headRotate = true;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapTeammateColor", desc = "config.babyzombieaddons.option.kuudraMinimapTeammateColor.desc") @SearchTag("minimap") @SearchTag("teammate") @SearchTag("color")
        public ChromaColour teammateColor = ChromaColour.fromStaticRGB(255, 255, 255, 255);
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapSelfColor", desc = "config.babyzombieaddons.option.kuudraMinimapSelfColor.desc") @SearchTag("minimap") @SearchTag("self") @SearchTag("color")
        public ChromaColour selfColor = ChromaColour.fromStaticRGB(0, 255, 0, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapKuudra", desc = "config.babyzombieaddons.option.kuudraMinimapKuudra.desc") @ConfigEditorBoolean @SearchTag("minimap") @SearchTag("kuudra")
        public boolean showKuudra = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapTentacles", desc = "config.babyzombieaddons.option.kuudraMinimapTentacles.desc") @ConfigEditorBoolean @SearchTag("minimap") @SearchTag("tentacle")
        public boolean tentacles = false;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapTentacleColor", desc = "config.babyzombieaddons.option.kuudraMinimapTentacleColor.desc") @SearchTag("minimap") @SearchTag("tentacle") @SearchTag("color")
        public ChromaColour tentacleColor = ChromaColour.fromStaticRGB(255, 140, 40, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapDropships", desc = "config.babyzombieaddons.option.kuudraMinimapDropships.desc") @ConfigEditorBoolean @SearchTag("minimap") @SearchTag("dropship") @SearchTag("ghast")
        public boolean dropships = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapCannons", desc = "config.babyzombieaddons.option.kuudraMinimapCannons.desc") @ConfigEditorBoolean @SearchTag("minimap") @SearchTag("cannon")
        public boolean cannons = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapSupplies", desc = "config.babyzombieaddons.option.kuudraMinimapSupplies.desc") @ConfigEditorBoolean @SearchTag("minimap") @SearchTag("supply")
        public boolean supplies = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapPiles", desc = "config.babyzombieaddons.option.kuudraMinimapPiles.desc") @ConfigEditorBoolean @SearchTag("minimap") @SearchTag("pile")
        public boolean piles = true;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapPileColor", desc = "config.babyzombieaddons.option.kuudraMinimapPileColor.desc") @SearchTag("minimap") @SearchTag("pile") @SearchTag("color")
        public ChromaColour pileColor = ChromaColour.fromStaticRGB(229, 229, 46, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapBallista", desc = "config.babyzombieaddons.option.kuudraMinimapBallista.desc") @ConfigEditorBoolean @SearchTag("minimap") @SearchTag("ballista")
        public boolean ballista = true;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapBallistaColor", desc = "config.babyzombieaddons.option.kuudraMinimapBallistaColor.desc") @SearchTag("minimap") @SearchTag("ballista") @SearchTag("color")
        public ChromaColour ballistaColor = ChromaColour.fromStaticRGB(229, 229, 46, 255);
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapPileDoneColor", desc = "config.babyzombieaddons.option.kuudraMinimapPileDoneColor.desc") @SearchTag("minimap") @SearchTag("pile") @SearchTag("color")
        public ChromaColour pileDoneColor = ChromaColour.fromStaticRGB(85, 85, 85, 255);
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapFuel", desc = "config.babyzombieaddons.option.kuudraMinimapFuel.desc") @ConfigEditorBoolean @SearchTag("minimap") @SearchTag("fuel")
        public boolean fuel = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapChucks", desc = "config.babyzombieaddons.option.kuudraMinimapChucks.desc") @ConfigEditorBoolean @SearchTag("minimap") @SearchTag("chuck")
        public boolean chucks = false;
        @Expose @ConfigEditorColour @ConfigOption(name = "config.babyzombieaddons.option.kuudraMinimapCannonColor", desc = "config.babyzombieaddons.option.kuudraMinimapCannonColor.desc") @SearchTag("minimap") @SearchTag("cannon") @SearchTag("color")
        public ChromaColour cannonColor = ChromaColour.fromStaticRGB(200, 200, 208, 255);
    }
}
