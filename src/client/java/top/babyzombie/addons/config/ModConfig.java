package top.babyzombie.addons.config;

public class ModConfig {

    public enum ConfigBackend { YACL, MOUL_CONFIG }
    public enum AutoISDest { ISLAND, GARDEN }
    public enum KickRecovery { OFF, LOBBY_ONLY, LOBBY_AND_SKYBLOCK }
    public enum RequeueMode { OFF, ON_FAIL, ON_WIN, ALWAYS }
    public enum CrowdHideMode { OFF, HIDE, REMOVE }
    public enum HpDisplayMode { OFF, HUD, BOSSBAR }
    public enum MineshaftWarpMode { OFF, TITLE_ONLY, TITLE_AND_SOUND, SEND_PTME, PTME_AND_WARP }

    // Slayer info levels
    public enum SlayerInfoLevel0_2 { OFF, HP_ONLY, HP_AND_STATUS }
    public enum SlayerInfoLevel0_1 { OFF, HP_ONLY }
    public enum SlayerInfoLevel0_3 { OFF, HP_ONLY, HP_AND_STATUS, HP_STATUS_REPEAT }
    public enum BoxSlayerMode { OFF, GLOW, GLOW_AND_BEAM }
    public enum GummyPolarBearMode { OFF, SMOLDERING_TOMB_ONLY, EVERYWHERE_EXCEPT_DUNGEON }
    public enum RagnarockAxeMode { OFF, NUMERIC, PROGRESS_BAR }
    public enum EndStoneSwordMode { OFF, TIMER_ONLY, PREVENT_REUSE, BOTH }

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
    public DebugConfig debug = new DebugConfig();

    // ---- General ----

    public static class GeneralConfig {
        public boolean autois = false;
        public int autoisDelay = 5;
        public AutoISDest autoisDest = AutoISDest.GARDEN;
        public KickRecovery autoBackToSkyblock = KickRecovery.LOBBY_ONLY;
        public boolean cakeBuffTracker = false;
    }

    // ---- Dungeon ----

    public static class DungeonConfig {
        public CrowdHideMode f4CrowdHiding = CrowdHideMode.OFF;
        public RequeueMode autoRequeue = RequeueMode.OFF;
        public int requeueDelay = 0;
        public String requeueMessage = "going in %delay%";
        public String requeueCancelMessage = "ok";
        public String requeueCancelKeywords = "c|cancel|n|nr|wait|stop|dt|don't|gtg|tyfr|tyfrs|gtg tyfr|gtg tyfrs|no key|别急|等会|等下|先别开";
        public boolean autoChestClose = false;
        public boolean dailyCounter = false;
    }

    // ---- Kuudra ----

    public static class KuudraConfig {
        public HpDisplayMode hpDisplay = HpDisplayMode.OFF;
        public boolean phaseTimer = false;
        public boolean stunTimer = false;
        public boolean waypoints = false;
        public boolean energyDisplay = false;
        public boolean directionIndicator = false;
        public boolean boxKuudra = false;
        public boolean enderPearlRefill = false;
        public boolean perkShopBlacklist = false;
        public String perkShopBlacklistItems = "Elle's Pickaxe,Elle's Lava Rod,Auto Revive,Support Route,Mining Frenzy I";
        public boolean followerHelmetPrice = false;
    }

    // ---- Slayer ----

    public static class SlayerConfig {
        public boolean pigmanSwordTimer = false;
        public boolean holyIceTimer = false;
        public RagnarockAxeMode ragnarockAxeTimer = RagnarockAxeMode.OFF;
        public boolean reaperArmorTimer = false;
        public EndStoneSwordMode endStoneSwordTimer = EndStoneSwordMode.OFF;
        public boolean bossInfoHUD = false;

        public boolean noslayerquest = false;
        public GummyPolarBearMode reheatedGummyPolarBear = GummyPolarBearMode.OFF;
        public BoxSlayerMode boxslayerboss = BoxSlayerMode.OFF;
        public java.awt.Color boxbosscolor = new java.awt.Color(255, 0, 0, 255);
        public boolean boxLowHPBloodfiend = false;
        public boolean showEffigies = false;

        public SlayerInfoLevel0_2 zombieSlayerInfo = SlayerInfoLevel0_2.HP_AND_STATUS;
        public SlayerInfoLevel0_2 spiderSlayerInfo = SlayerInfoLevel0_2.HP_AND_STATUS;
        public SlayerInfoLevel0_2 wolfSlayerInfo = SlayerInfoLevel0_2.HP_AND_STATUS;
        public SlayerInfoLevel0_2 endermanSlayerInfo = SlayerInfoLevel0_2.HP_AND_STATUS;
        public SlayerInfoLevel0_3 blazeSlayerInfo = SlayerInfoLevel0_3.HP_AND_STATUS;
        public SlayerInfoLevel0_2 vampireSlayerInfo = SlayerInfoLevel0_2.HP_AND_STATUS;
    }

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
        public int chestLineWidth = 3;
        public boolean getFromSacks = false;
        public boolean scathaCooldown = false;
        public boolean armadilloEnergy = false;
        public boolean darkMonolithFinder = false;
        public boolean drillSwingSuppression = false;
        public boolean powderMiningSounds = false;


        public boolean mineshaftWaypoints = false;
        public MineshaftWarpMode glaciteMineshaftWarp = MineshaftWarpMode.OFF;
        public boolean suspiciousScrapCounter = false;
        public boolean creeperVisibility = false;
        public boolean greatGlaciteWaypoints = false;
    }

    // ---- Garden ----

    public static class GardenConfig {
        public boolean pestDisplay = false;
        public int xpOrbSoundRemoval = 100;
        public boolean signAutoRotate = false;
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

    // ---- Party ----

    public static class PartyConfig {
        public boolean partyAllinvite = false;
        public boolean partyInvite = false;
        public boolean partyWarp = false;
        public boolean partyWarpDelay = false;
        public int partyWarpDelaySeconds = 3;
        public boolean partyJoinInstance = false;
        public boolean partySendCoords = false;
        public boolean partyTransfer = false;
        public boolean dmPartyInvite = false;
    }

    // ---- Events ----

    public static class EventsConfig {
        public boolean greatSpook = false;
        public boolean greatSpookDelay = false;
        public String publicSpeakingDemon = "";
        public boolean fruitDiggingHelper = false;
        public boolean fruitDiggingAutoAccept = false;
    }

    // ---- Misc ----

    public static class MiscConfig {

        public boolean abiphoneGui = false;
        public boolean playCmd = false;
    }

    // ---- Debug ----

    public static class DebugConfig {
        public boolean debugMode = false;
        public ConfigBackend configBackend = ConfigBackend.MOUL_CONFIG;
    }
}
