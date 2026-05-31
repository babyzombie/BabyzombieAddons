package top.babyzombie.addons.config;

public class ModConfig {

    public enum ConfigBackend { YACL, MOUL_CONFIG }

    public GeneralConfig general = new GeneralConfig();
    public DungeonConfig dungeon = new DungeonConfig();
    public KuudraConfig kuudra = new KuudraConfig();
    public SlayerConfig slayer = new SlayerConfig();
    public WitherCloakConfig witherCloak = new WitherCloakConfig();
    public MiningConfig mining = new MiningConfig();
    public FishingConfig fishing = new FishingConfig();
    public GardenConfig garden = new GardenConfig();
    public PartyConfig party = new PartyConfig();
    public PopupConfig popup = new PopupConfig();
    public MiscConfig misc = new MiscConfig();
    public DebugConfig debug = new DebugConfig();

    // ---- General ----

    public static class GeneralConfig {
        public boolean autois = false;
        public boolean autoUpdateCheck = false;
        public boolean noFog = false;
        public boolean doubleLobby = false;
        public boolean autoEnglish = false;
        public boolean hideBlockMessages = false;
        public boolean crimsonArmorMute = false;
        public boolean cancelEnderPearl = false;
        public boolean cakeBuffTracker = false;
        public boolean itemTimestamp = false;
        public boolean betterSignEditing = false;
        public boolean quickAuction = false;
        public int witherShieldTimerMode = 1;
        public boolean hideClosePlayers = false;
        public int closePlayerRadius = 5;
        public boolean vanquisherAlert = false;
        public boolean autoAbiphoneAnswer = false;
        public boolean jerryBoxHelper = false;
        public boolean dailyChineseTranslation = false;
    }

    // ---- Dungeon ----

    public static class DungeonConfig {
        public boolean welcomeTitle = false;
        public boolean bloodReadyAlert = false;
        public boolean witherKeyMarkers = false;
        public boolean dupeArcherDetection = false;
        public boolean boxStarMobs = false;
        public boolean boxFels = false;
        public boolean f4CrowdHiding = false;
        public boolean stormThunderMuting = false;
        public boolean autoChestClose = false;
        public boolean noAligned = false;
        public boolean dailyCounter = false;
        public boolean autoRequeue = false;
        public boolean instanceWarp = false;
        public int readyCheckDelay = 10;
    }

    // ---- Kuudra ----

    public static class KuudraConfig {
        public boolean welcomeTitle = false;
        public boolean hpDisplay = false;
        public boolean phaseTimer = false;
        public boolean dropshipWarning = false;
        public boolean wanderingBlazesWarning = false;
        public boolean stunTimer = false;
        public boolean waypoints = false;
        public boolean energyDisplay = false;
        public boolean directionIndicator = false;
        public boolean boxKuudra = false;
        public boolean enderPearlRefill = false;
        public boolean perkShopBlacklist = false;
        public boolean extremeFocusWarning = false;
        public boolean followerHelmetPrice = false;
    }

    // ---- Slayer ----

    public static class SlayerConfig {
        public boolean noQuestReminder = false;
        public boolean pigmanSwordTimer = false;
        public boolean ragnarockAxeTimer = false;
        public boolean reaperArmorTimer = false;
        public boolean endStoneSwordTimer = false;
        public boolean bossInfoHUD = false;
        public boolean bossBoundingBox = false;
        public boolean lowHPBloodfiend = false;
        public boolean riftEffigyDisplay = false;
        public boolean gummyBearTimer = false;
    }

    // ---- Wither Cloak ----

    public static class WitherCloakConfig {
        public boolean witherCloakTimer = false;
        public boolean soulwardTimer = false;
        public boolean alignedTimer = false;
        public boolean gravityStormTimer = false;
    }

    // ---- Mining ----

    public static class MiningConfig {
        public boolean nucleusAutoWarp = false;
        public boolean miningAbilityAlerts = false;
        public boolean crystalHollowsPassAutoRenew = false;
        public boolean chestMarkers = false;
        public boolean getFromSacks = false;
        public boolean scathaCooldown = false;
        public boolean armadilloEnergy = false;
        public boolean compassSolver = false;
        public boolean darkMonolithFinder = false;
        public boolean drillSwingSuppression = false;
        public boolean powderMiningSounds = false;
        public boolean noPickobulus = false;
        public boolean glaciteWaypoints = false;
        public boolean mineshaftWaypoints = false;
        public boolean suspiciousScrapCounter = false;
        public boolean baseCampQuickWarp = false;
    }

    // ---- Fishing ----

    public static class FishingConfig {
        public boolean legendaryAlerts = false;
        public boolean volcanoSteamReduction = false;
        public boolean slugfishHookLock = false;
        public boolean killInvisibleGoldenFish = false;
        public boolean reindrakeHP = false;
    }

    // ---- Garden ----

    public static class GardenConfig {
        public boolean pestDisplay = false;
        public boolean xpOrbSoundRemoval = false;
        public boolean signAutoRotate = false;
    }

    // ---- Party ----

    public static class PartyConfig {
        public boolean autoAccept = false;
        public boolean doublePWarpConfirm = false;
        public boolean partyAllinvite = false;
        public boolean partyInvite = false;
        public boolean partyWarp = false;
        public boolean partyWarpDelay = false;
        public int partyWarpDelaySeconds = 3;
        public boolean partyJoinInstance = false;
        public boolean partySendCoords = false;
        public boolean dmPartyInvite = false;
    }

    // ---- Popup Events ----

    public static class PopupConfig {
        public boolean popupPartyInvite = false;
        public boolean popupGuildPartyInvite = false;
        public boolean popupFriendRequest = false;
        public boolean popupDuelsRequest = false;
        public boolean popupSkyblockTrade = false;
        public boolean popupDungeonRestart = false;
    }

    // ---- Misc ----

    public static class MiscConfig {
        public boolean killComboHUD = false;
        public boolean betterPerspective = false;
        public boolean dukeWaypoint = false;
        public boolean creeperVisibility = false;
        public boolean fruitDiggingHelper = false;
        public boolean bazaarSackExtraction = false;
        public boolean personalCompactorPreview = false;
        public boolean hideHyperionExplosion = false;
        public boolean witherImpactVolume = false;
        public boolean dyedArmorSBID = false;
        public boolean sackItemHUD = false;
        public boolean periodicEntityCleanup = false;
        public boolean stonksPrice = false;
        public boolean bitsShopPrice = false;
        public boolean attributeDisplay = false;
        public boolean timeChime = false;
        public boolean abiphoneGui = false;
        public boolean playCmd = false;
    }

    // ---- Debug ----

    public static class DebugConfig {
        public boolean debugMode = false;
        public ConfigBackend configBackend = ConfigBackend.MOUL_CONFIG;
    }
}
