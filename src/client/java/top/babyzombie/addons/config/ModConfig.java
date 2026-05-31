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
    public MiscConfig misc = new MiscConfig();
    public DebugConfig debug = new DebugConfig();

    // ---- General ----

    public static class GeneralConfig {
        public boolean autoUpdateCheck = true;
        public boolean noFog = true;
        public boolean doubleLobby = true;
        public boolean autoEnglish = false;
        public boolean hideBlockMessages = true;
        public boolean crimsonArmorMute = false;
        public boolean cancelEnderPearl = true;
        public boolean cakeBuffTracker = true;
        public boolean itemTimestamp = false;
        public boolean betterSignEditing = true;
        public boolean quickAuction = true;
        public int witherShieldTimerMode = 1;
        public boolean hideClosePlayers = false;
        public int closePlayerRadius = 5;
        public boolean vanquisherAlert = true;
        public boolean autoAbiphoneAnswer = true;
        public boolean jerryBoxHelper = true;
        public boolean dailyChineseTranslation = true;
    }

    // ---- Dungeon ----

    public static class DungeonConfig {
        public boolean welcomeTitle = true;
        public boolean bloodReadyAlert = true;
        public boolean witherKeyMarkers = true;
        public boolean dupeArcherDetection = true;
        public boolean boxStarMobs = true;
        public boolean boxFels = true;
        public boolean f4CrowdHiding = true;
        public boolean stormThunderMuting = true;
        public boolean autoChestClose = true;
        public boolean noAligned = true;
        public boolean dailyCounter = true;
        public boolean autoRequeue = true;
        public boolean instanceWarp = false;
        public int readyCheckDelay = 10;
    }

    // ---- Kuudra ----

    public static class KuudraConfig {
        public boolean welcomeTitle = true;
        public boolean hpDisplay = true;
        public boolean phaseTimer = true;
        public boolean dropshipWarning = true;
        public boolean wanderingBlazesWarning = true;
        public boolean stunTimer = true;
        public boolean waypoints = true;
        public boolean energyDisplay = true;
        public boolean directionIndicator = true;
        public boolean boxKuudra = true;
        public boolean enderPearlRefill = true;
        public boolean perkShopBlacklist = true;
        public boolean extremeFocusWarning = true;
        public boolean followerHelmetPrice = true;
    }

    // ---- Slayer ----

    public static class SlayerConfig {
        public boolean noQuestReminder = true;
        public boolean pigmanSwordTimer = true;
        public boolean ragnarockAxeTimer = true;
        public boolean reaperArmorTimer = true;
        public boolean endStoneSwordTimer = true;
        public boolean bossInfoHUD = true;
        public boolean bossBoundingBox = true;
        public boolean lowHPBloodfiend = true;
        public boolean riftEffigyDisplay = true;
        public boolean gummyBearTimer = true;
    }

    // ---- Wither Cloak ----

    public static class WitherCloakConfig {
        public boolean witherCloakTimer = true;
        public boolean soulwardTimer = true;
        public boolean alignedTimer = true;
        public boolean gravityStormTimer = true;
    }

    // ---- Mining ----

    public static class MiningConfig {
        public boolean nucleusAutoWarp = true;
        public boolean miningAbilityAlerts = true;
        public boolean crystalHollowsPassAutoRenew = true;
        public boolean chestMarkers = true;
        public boolean getFromSacks = true;
        public boolean scathaCooldown = true;
        public boolean armadilloEnergy = true;
        public boolean compassSolver = true;
        public boolean darkMonolithFinder = true;
        public boolean drillSwingSuppression = true;
        public boolean powderMiningSounds = true;
        public boolean noPickobulus = true;
        public boolean glaciteWaypoints = true;
        public boolean mineshaftWaypoints = true;
        public boolean suspiciousScrapCounter = true;
        public boolean baseCampQuickWarp = true;
    }

    // ---- Fishing ----

    public static class FishingConfig {
        public boolean legendaryAlerts = true;
        public boolean volcanoSteamReduction = true;
        public boolean slugfishHookLock = true;
        public boolean killInvisibleGoldenFish = true;
        public boolean reindrakeHP = true;
    }

    // ---- Garden ----

    public static class GardenConfig {
        public boolean pestDisplay = true;
        public boolean xpOrbSoundRemoval = true;
        public boolean signAutoRotate = true;
    }

    // ---- Party ----

    public static class PartyConfig {
        public boolean autoAccept = true;
        public boolean doublePWarpConfirm = true;
        public boolean partyCommands = true;
    }

    // ---- Misc ----

    public static class MiscConfig {
        public boolean killComboHUD = true;
        public boolean betterPerspective = true;
        public boolean dukeWaypoint = true;
        public boolean creeperVisibility = true;
        public boolean fruitDiggingHelper = true;
        public boolean bazaarSackExtraction = true;
        public boolean personalCompactorPreview = true;
        public boolean hideHyperionExplosion = false;
        public boolean witherImpactVolume = false;
        public boolean dyedArmorSBID = true;
        public boolean sackItemHUD = false;
        public boolean periodicEntityCleanup = false;
        public boolean stonksPrice = true;
        public boolean bitsShopPrice = true;
        public boolean attributeDisplay = true;
        public boolean timeChime = false;
        public boolean abiphoneGui = true;
        public boolean playCmd = true;
    }

    // ---- Debug ----

    public static class DebugConfig {
        public boolean debugMode = false;
        public ConfigBackend configBackend = ConfigBackend.MOUL_CONFIG;
    }
}
