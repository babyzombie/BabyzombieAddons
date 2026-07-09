package top.babyzombie.addons.config;

public class ModConfig {

    public enum ConfigBackend { YACL, MOUL_CONFIG }
    public enum AutoISDest { ISLAND, GARDEN }
    public enum KickRecovery { OFF, LOBBY_ONLY, LOBBY_AND_SKYBLOCK }
    public enum RequeueMode { OFF, ON_FAIL, ON_WIN, ALWAYS }
    public enum CrowdHideMode { OFF, HIDE, REMOVE }
    public enum DailyCounterMode { OFF, FIRST_5, ALWAYS }
    public enum DeathMessageAction { OFF, COPY, SEND, COPY_AND_SEND }
    public enum HpDisplayMode { OFF, HUD, BOSSBAR }
    public enum MineshaftWarpMode { OFF, TITLE_ONLY, TITLE_AND_SOUND, SEND_PTME, PTME_AND_WARP }

    public enum GummyPolarBearMode { OFF, SMOLDERING_TOMB_ONLY, EVERYWHERE_EXCEPT_DUNGEON }
    public enum RagnarockAxeMode { OFF, NUMERIC, PROGRESS_BAR }
    public enum EndStoneSwordMode { OFF, TIMER_ONLY, PREVENT_REUSE, BOTH }
    public enum SlayerBossInfoMode { OFF, BASIC, FULL }
    public enum SlayerBossBoxMode { OFF, WIREFRAME, BOX }
    public enum WorldRenderPhase { AFTER_ENTITIES, END_MAIN }
    public enum BzGetFromSacksMode { OFF, GET_ONLY, GET_AND_RECLICK }
    public enum AutoPotionsMode { OFF, M4, M5, M6, M7 }

    public enum ToxicArrowMinTier { T1, T2, T3, T4, T5 }

    public enum ToxicArrowTiming {
        KUUDRA_START,
        SUPPLIES_DONE,
        BALLISTA_READY,
        STUNNER_ENTER,
        KUUDRA_STUNNED
    }

    public enum TwilightArrowTiming {
        KUUDRA_START,
        SUPPLIES_DONE,
        BALLISTA_READY,
        KUUDRA_STUNNED,
        P4_START,
        P4_SHORTLY_AFTER,
        P4_TRUE_LAIR
    }

    public GeneralConfig general = new GeneralConfig();
    public DungeonConfig dungeon = new DungeonConfig();
    public KuudraConfig kuudra = new KuudraConfig();
    public SlayerConfig slayer = new SlayerConfig();
    public WitherCloakConfig witherCloak = new WitherCloakConfig();
    public PartyConfig party = new PartyConfig();
    public MiningConfig mining = new MiningConfig();
    public GardenConfig garden = new GardenConfig();
    public PopupConfig popup = new PopupConfig();
    public EventsConfig events = new EventsConfig();
    public MiscConfig misc = new MiscConfig();
    public ChatChannelConfig chatChannel = new ChatChannelConfig();
    public DebugConfig debug = new DebugConfig();
    public AutoJoinConfig autoJoin = new AutoJoinConfig();
    public HandRenderConfig handRender = new HandRenderConfig();
    public PetConfig pet = new PetConfig();
    public AutotipConfig autotip = new AutotipConfig();
    public WindowTitleConfig windowTitle = new WindowTitleConfig();

    // ---- General ----

    public static class GeneralConfig {
        public boolean updateChecker = true;
        public boolean autois = false;
        public int autoisDelay = 5;
        public AutoISDest autoisDest = AutoISDest.GARDEN;
        public KickRecovery autoBackToSkyblock = KickRecovery.LOBBY_ONLY;
        public boolean hideEntities = false;
        public boolean backOnServerRestart = false;
        public boolean cakeBuffTracker = false;
        public boolean minionCollectAutoClose = false;
        public int skipSecondPerson = 0;
        public boolean useTpsAdjustedTime = false;
        public WorldRenderPhase renderPhase = WorldRenderPhase.AFTER_ENTITIES;
        public boolean autoReconnectEnabled = false;
        public int autoReconnectDelay = 5;
        public int autoReconnectMaxRetries = 0;
        public float playerScaleX = 1.0f;
        public float playerScaleY = 1.0f;
        public float playerScaleZ = 1.0f;
        public boolean showCrosshairInThirdPerson = false;
        public boolean chatInContainer = false;
        public boolean replaceReportWithServerList = false;
        public boolean serverResourcePackAutoAccept = false;
    }

    // ---- Dungeon ----

    public static class DungeonConfig {
        public CrowdHideMode f4CrowdHiding = CrowdHideMode.OFF;
        public RequeueMode dungeonRequeue = RequeueMode.OFF;
        public int dungeonRequeueDelay = 0;
        public RequeueMode kuudraRequeue = RequeueMode.OFF;
        public int kuudraRequeueDelay = 0;
        public String requeueMessage = "going in %delay%";
        public String requeueCancelMessage = "ok";
        public String requeueCancelKeywords = "c|cancel|n|nr|wait|stop|dt|don't|gtg|tyfr|tyfrs|gtg tyfr|gtg tyfrs|no key|别急|等会|等下|先别开";
        public boolean enderPearlRefill = false;
        public boolean autoChestClose = false;
        public boolean muteStormThunder = false;
        public boolean muteRareDropSound = false;
        public AutoPotionsMode autoOpenPotions = AutoPotionsMode.OFF;
        public DailyCounterMode dailyRunsCounter = DailyCounterMode.OFF;
        public DeathMessageAction deathMessageAction = DeathMessageAction.OFF;
    }

    // ---- Kuudra ----

    public static class KuudraConfig {
        public HpDisplayMode hpDisplay = HpDisplayMode.OFF;
        public boolean phaseTimer = false;
        public boolean stunTimer = false;
        public boolean supplyBeacons = false;
        public int supplyBeaconColor = 0xFF00FF00;
        public boolean supplyDropoffBeacons = false;
        public int supplyDropoffBeaconColor = 0xFFFFFF00;
        public boolean ballistaProgressText = false;
        public int ballistaTextColor = 0xFFFFFF55;
        public boolean ballistaBuildBeacons = false;
        public int ballistaBeaconColor = 0xFF4C7FFF;
        public boolean fuelOrbBeacons = false;
        public int fuelOrbBeaconColor = 0xFFFF0000;
        public boolean energyDisplay = false;
        public boolean boxKuudra = false;
        public boolean enderPearlRefill = false;
        public boolean perkShopBlacklist = false;
        public String perkShopBlacklistItems = "Elle's Pickaxe,Elle's Lava Rod,Auto Revive,Support Route,Mining Frenzy I";
        public boolean followerHelmetPrice = false;
        public boolean muteCrimsonArmor = false;
        public boolean nopeMagmafish = false;
        public ToxicArrowMinTier toxicArrowMinTier = ToxicArrowMinTier.T3;
        public ToxicArrowTiming toxicArrowTiming = ToxicArrowTiming.KUUDRA_STUNNED;
        public int toxicArrowThreshold = 0;
        public int toxicArrowPerMissing = 0;
        public TwilightArrowTiming twilightArrowTiming = TwilightArrowTiming.P4_START;
        public int twilightArrowThreshold = 0;
    }

    // ---- Slayer ----

    public static class SlayerConfig {
        public boolean pigmanSwordTimer = false;
        public boolean holyIceTimer = false;
        public RagnarockAxeMode ragnarockAxeTimer = RagnarockAxeMode.OFF;
        public boolean reaperArmorTimer = false;
        public EndStoneSwordMode endStoneSwordTimer = EndStoneSwordMode.OFF;
        public boolean noslayerquest = false;
        public GummyPolarBearMode reheatedGummyPolarBear = GummyPolarBearMode.OFF;
        public boolean boxLowHPBloodfiend = false;
        public boolean showEffigies = false;
        public SlayerBossInfoMode zombieSlayerInfo = SlayerBossInfoMode.OFF;
        public SlayerBossInfoMode spiderSlayerInfo = SlayerBossInfoMode.OFF;
        public SlayerBossInfoMode wolfSlayerInfo = SlayerBossInfoMode.OFF;
        public SlayerBossInfoMode endermanSlayerInfo = SlayerBossInfoMode.OFF;
        public SlayerBossInfoMode blazeSlayerInfo = SlayerBossInfoMode.OFF;
        public SlayerBossInfoMode vampireSlayerInfo = SlayerBossInfoMode.OFF;
        public SlayerBossBoxMode boxSlayerBoss = SlayerBossBoxMode.OFF;
        public int boxBossColor = 0xFFFFFFFF;
        public int boxBossBeamColor = 0xFFFFFFFF;
        public boolean boxBossBeam = false;
        public boolean boxBossRenderThroughWalls = false;
        public int boxBossLineWidth = 5;
    }

    public static class WitherCloakConfig {
        public boolean witherCloakTimer = false;
        public boolean soulwardTimer = false;
        public boolean alignedTimer = false;
        public boolean gravityStormTimer = false;
        public boolean hideChargedCreepers = false;
    }

    // ---- Mining ----

    public static class MiningConfig {
        public boolean nucleusAutoWarp = false;
        public boolean miningAbilityAlerts = false;
        public boolean crystalHollowsPassAutoRenew = false;
        public boolean chestMarkers = false;
        public int chestLineWidth = 3;
        public boolean getFromSacks = false;
        public boolean scathaCooldown = false;
        public boolean scathaReleaseKey = false;
        public boolean armadilloEnergy = false;
        public boolean drillSwingSuppression = false;
        public boolean powderMiningSounds = false;


        public boolean mineshaftWaypoints = false;
        public MineshaftWarpMode glaciteMineshaftWarp = MineshaftWarpMode.OFF;
        public boolean suspiciousScrapCounter = false;
        public boolean creeperVisibility = false;
        public boolean greatGlaciteWaypoints = false;
        public boolean jungleTempleThinWall = false;
    }

    // ---- Garden ----

    public static class GardenConfig {
        public boolean pestDisplay = false;
        public int xpOrbSoundRemoval = 100;
        public boolean signAutoRotate = false;
        public boolean trevorAutoAccept = false;
        public boolean trevorAutoCall = false;
        public boolean farmingToolSwingSuppression = false;
    }

    // ---- Popup Events ----

    public static class PopupConfig {
        public boolean popupPartyInvite = false;
        public boolean popupGuildPartyInvite = false;
        public boolean popupFriendRequest = false;
        public boolean popupDuelsRequest = false;
        public boolean popupSkyblockTrade = false;
        public boolean popupDungeonRestart = false;
        public top.babyzombie.addons.module.popup.PopupEventsModule.PopupSound popupSound =
                top.babyzombie.addons.module.popup.PopupEventsModule.PopupSound.BELL;
        public int popupBaitLow = 0;
    }

    // ---- Party ----

    public static class PartyConfig {
        public boolean autoAcceptReparty = false;
        public boolean partyAllinvite = false;
        public boolean partyInvite = false;
        public boolean partyWarp = false;
        public boolean partyWarpDelay = false;
        public int partyWarpDelaySeconds = 3;
        public boolean partyJoinInstance = false;
        public boolean partySendCoords = false;
        public boolean partyTransfer = false;
        public boolean dmPartyInvite = false;
        public boolean partyPlay = false;
        public boolean partyStream = false;
        public boolean partySelfExecute = false;
    }

    // ---- Events ----

    public static class EventsConfig {
        public boolean greatSpook = false;
        public boolean greatSpookDelay = false;
        public String publicSpeakingDemon = "";
        public boolean fruitDiggingHelper = false;
        public boolean carnivalAutoAccept = false;

        // ── Fruit Digging Solver ──
        public boolean fruitDiggingSolver = false;
        public float solverBombPenalty = 200f;
        public float solverRumPenalty = 100f;
        public float solverMinesInfoWeight = 3f;
        public float solverTreasureInfoWeight = 2f;
        public float solverAnchorInfoWeight = 1f;
        public float solverEarlyAppleBonus = 50f;
        public float solverEarlyCherryBonus = 80f;
        public int solverLateGameDigs = 3;

        // ── Anniversary ──
        public boolean raffleTaskTracker = false;
    }

    // ---- Misc ----

    public static class MiscConfig {

        public boolean abiphoneGui = false;
        public boolean playCmd = false;
        public BzGetFromSacksMode bzGetFromSacks = BzGetFromSacksMode.OFF;
        public int serverVisitExpireMinutes = 10;

        // Necron's Blade
        public float necronBladeExplosionVolume = 1.0f;
        public boolean necronBladeHideExplosionParticles = false;
        public boolean necronBladeHideOthersParticles = false;
        public boolean showClickEventInHover = false;
    }

    // ---- Chat Channel ----

    public static class ChatChannelConfig {
        public boolean chatChannelSwitcher = false;
    }

    // ---- Debug ----

    public static class DebugConfig {
        public boolean debugMode = false;
        public boolean hypixelModApiDebugLog = false;
        public ConfigBackend configBackend = ConfigBackend.MOUL_CONFIG;
        public boolean wideMoulConfig = false;
        public int maxDebugEntities = 20;
    }

    // ---- Auto Join ----

    public static class AutoJoinConfig {
        public boolean autoJoinServer = false;
        public String autoJoinServerIP = "";
    }

    // ---- Pet Display HUD ----

    public static class PetConfig {
        public boolean petDisplay = false;
        public boolean petExpDisplay = true;
        public boolean petItemDisplay = true;
        public boolean petItemIconDisplay = true;
        public boolean petSharedDisplay = true;
    }

    // ---- Hand Render ----

    public static class HandRenderConfig {
        public boolean disableAll = false;
        public boolean swapHands = false;
        public boolean customSwingDuration = false;
        public int swingDurationTicks = 6;
        public float itemScale = 1.0f;
        public float itemOffsetX = 0.0f;
        public float itemOffsetY = 0.0f;
    }

    // ---- Auto Tip ----

    public static class AutotipConfig {
        public boolean enabled = false;
        public boolean hideMessages = false;
    }

    // ---- Window Title ----

    public static class WindowTitleConfig {
        public boolean enabled = false;
        public boolean overrideOriginal = false;
        public int updateInterval = 1;
        public boolean showMemory = false;
        public boolean showSystemMemory = false;
        public boolean showPing = false;
        public boolean showLocation = false;
    }
}
