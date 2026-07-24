package top.babyzombie.addons.config;

import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.Social;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import com.google.gson.annotations.Expose;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class ModConfig extends Config {

    // ── Shared Enums (toString() returns translated text for MoulConfig dropdowns) ──

    public enum AutoISDest {
        ISLAND, GARDEN;
        @Override public String toString() { return t("config.babyzombieaddons.option.autoisDest." + name()); }
    }
    public enum KickRecovery {
        OFF, LOBBY_ONLY, LOBBY_AND_SKYBLOCK;
        @Override public String toString() { return t("config.babyzombieaddons.option.autoBackToSkyblock." + name()); }
    }
    public enum RequeueMode {
        OFF, ON_FAIL, ON_WIN, ALWAYS;
        @Override public String toString() { return t("config.babyzombieaddons.option.requeueMode." + name()); }
    }
    public enum CrowdHideMode {
        OFF, HIDE, REMOVE;
        @Override public String toString() { return t("config.babyzombieaddons.option.f4CrowdHiding." + name()); }
    }
    public enum DailyCounterMode {
        OFF, FIRST_5, ALWAYS;
        @Override public String toString() { return t("config.babyzombieaddons.option.dailyRunsCounter." + name()); }
    }
    public enum DeathMessageAction {
        OFF, COPY, SEND, COPY_AND_SEND;
        @Override public String toString() { return t("config.babyzombieaddons.option.deathMessageAction." + name()); }
    }
    public enum HpDisplayMode {
        OFF, HUD, BOSSBAR;
        @Override public String toString() { return t("config.babyzombieaddons.option.hpDisplay." + name()); }
    }
    public enum MineshaftWarpMode {
        OFF, TITLE_ONLY, TITLE_AND_SOUND, SEND_PTME, PTME_AND_WARP;
        @Override public String toString() { return t("config.babyzombieaddons.option.glaciteMineshaftWarp." + name()); }
    }
    public enum GummyPolarBearMode {
        OFF, SMOLDERING_TOMB_ONLY, EVERYWHERE_EXCEPT_DUNGEON;
        @Override public String toString() { return t("config.babyzombieaddons.option.reheatedGummyPolarBear." + name()); }
    }
    public enum RagnarockAxeMode {
        OFF, NUMERIC, PROGRESS_BAR;
        @Override public String toString() { return t("config.babyzombieaddons.option.ragnarockAxeTimer." + name()); }
    }
    public enum EndStoneSwordMode {
        OFF, TIMER_ONLY, PREVENT_REUSE, BOTH;
        @Override public String toString() { return t("config.babyzombieaddons.option.endStoneSwordTimer." + name()); }
    }
    public enum SlayerBossInfoMode {
        OFF, BASIC, FULL;
        @Override public String toString() { return t("config.babyzombieaddons.option.slayerBossInfoMode." + name()); }
    }
    public enum SlayerBossBoxMode {
        OFF, WIREFRAME, BOX;
        @Override public String toString() { return t("config.babyzombieaddons.option.boxSlayerBoss." + name()); }
    }
    public enum WorldRenderPhase {
        AFTER_ENTITIES, END_MAIN;
        @Override public String toString() { return t("config.babyzombieaddons.option.renderPhase." + name()); }
    }
    public enum BzGetFromSacksMode {
        OFF, GET_ONLY, GET_AND_RECLICK;
        @Override public String toString() { return t("config.babyzombieaddons.option.bzGetFromSacks." + name()); }
    }
    public enum AutoPotionsMode {
        OFF, M4, M5, M6, M7;
        @Override public String toString() { return t("config.babyzombieaddons.option.autoOpenPotions." + name()); }
    }
    public enum ToxicArrowMinTier {
        T1, T2, T3, T4, T5;
        @Override public String toString() { return t("config.babyzombieaddons.option.toxicArrowMinTier." + name()); }
    }
    public enum ToxicArrowTiming {
        KUUDRA_START, SUPPLIES_DONE, BALLISTA_READY, STUNNER_ENTER, KUUDRA_STUNNED;
        @Override public String toString() { return t("config.babyzombieaddons.option.toxicArrowTiming." + name()); }
    }
    public enum TwilightArrowTiming {
        KUUDRA_START, SUPPLIES_DONE, BALLISTA_READY, KUUDRA_STUNNED,
        P4_START, P4_SHORTLY_AFTER, P4_TRUE_LAIR;
        @Override public String toString() { return t("config.babyzombieaddons.option.twilightArrowTiming." + name()); }
    }
    public enum EntityRenderMode {
        ARMOR_STAND, FAKE_PLAYER, FAKE_PLAYER_EYES;
        @Override public String toString() { return t("config.babyzombieaddons.option.loadoutEntityRenderMode." + name()); }
    }
    public enum MusicDisc {
        DISC_5(178), DISC_11(71), DISC_13(178), BLOCKS(345), CAT(185), CHIRP(185), FAR(174),
        LAVA_CHICKEN(135), MALL(197), MELLOHI(96), PIGSTEP(148), STAL(150), STRAD(188),
        WAIT(237), WARD(251), OTHERSIDE(195), RELIC(219), CREATOR(176),
        CREATOR_MUSIC_BOX(73), PRECIPICE(299), TEARS(175);

        private final int durationSeconds;

        MusicDisc(int durationSeconds) {
            this.durationSeconds = durationSeconds;
        }

        /** 唱片时长（秒） */
        public int getDurationSeconds() {
            return durationSeconds;
        }

        /** 唱片名为专有名词，直接写死，不需翻译。§b = 淡蓝色 */
        @Override public String toString() {
            return switch (this) {
                case DISC_5 -> "§b5";
                case DISC_11 -> "§b11";
                case DISC_13 -> "§b13";
                case BLOCKS -> "§bblocks";
                case CAT -> "§bcat";
                case CHIRP -> "§bchirp";
                case FAR -> "§bfar";
                case LAVA_CHICKEN -> "§bLava Chicken";
                case MALL -> "§bmall";
                case MELLOHI -> "§bmellohi";
                case PIGSTEP -> "§bPigstep";
                case STAL -> "§bstal";
                case STRAD -> "§bstrad";
                case WAIT -> "§bwait";
                case WARD -> "§bward";
                case OTHERSIDE -> "§botherside";
                case RELIC -> "§bRelic";
                case CREATOR -> "§bCreator";
                case CREATOR_MUSIC_BOX -> "§bCreator (Music Box)";
                case PRECIPICE -> "§bPrecipice";
                case TEARS -> "§bTears";
            };
        }

        public Identifier getSoundId() {
            String n = name().toLowerCase().replace("disc_", "");
            return Identifier.fromNamespaceAndPath("minecraft", "music_disc." + n);
        }
    }
    public enum PlayMode {
        SINGLE, SEQUENTIAL, RANDOM;
        @Override public String toString() { return t("config.babyzombieaddons.option.playMode." + name()); }
    }

    /** Translates a key via Minecraft's I18n system. */
    private static String t(String key) {
        return Component.translatable(key).getString();
    }

    // ── Title & Social Links ──

    @Override
    public StructuredText getTitle() {
        return StructuredText.translatable("config.babyzombieaddons.title");
    }

    @Override
    public List<Social> getSocials() {
        return List.of(
                Social.forLink(
                        StructuredText.translatable("config.babyzombieaddons.social.github"),
                        new MyResourceLocation("babyzombieaddons", "textures/github.png"),
                        "https://github.com/babyzombie/BabyzombieAddons"
                ),
                Social.forLink(
                        StructuredText.translatable("config.babyzombieaddons.social.gitee"),
                        new MyResourceLocation("babyzombieaddons", "textures/gitee.png"),
                        "https://gitee.com/babyzombie/BabyzombieAddons"
                )
        );
    }

    // ── Categories ──

    @Expose
    @Category(name = "config.babyzombieaddons.category.general", desc = "")
    public GeneralConfig general = new GeneralConfig();

    @Expose
    @Category(name = "config.babyzombieaddons.category.skyblock", desc = "")
    public SkyblockConfig skyblock = new SkyblockConfig();

    @Expose
    @Category(name = "config.babyzombieaddons.category.dungeon", desc = "")
    public DungeonConfig dungeon = new DungeonConfig();

    @Expose
    @Category(name = "config.babyzombieaddons.category.kuudra", desc = "")
    public KuudraConfig kuudra = new KuudraConfig();

    @Expose
    @Category(name = "config.babyzombieaddons.category.slayer", desc = "")
    public SlayerConfig slayer = new SlayerConfig();

    @Expose
    @Category(name = "config.babyzombieaddons.category.hunting", desc = "")
    public HuntingConfig hunting = new HuntingConfig();

    @Expose
    @Category(name = "config.babyzombieaddons.category.mining", desc = "")
    public MiningConfig mining = new MiningConfig();

    @Expose
    @Category(name = "config.babyzombieaddons.category.fishing", desc = "")
    public FishingConfig fishing = new FishingConfig();

    @Expose
    @Category(name = "config.babyzombieaddons.category.garden", desc = "")
    public GardenConfig garden = new GardenConfig();

    @Expose
    @Category(name = "config.babyzombieaddons.category.party", desc = "")
    public PartyConfig party = new PartyConfig();

    @Expose
    @Category(name = "config.babyzombieaddons.category.popup", desc = "config.babyzombieaddons.category.popup.desc")
    public PopupConfig popup = new PopupConfig();

    @Expose
    @Category(name = "config.babyzombieaddons.category.events", desc = "")
    public EventsConfig events = new EventsConfig();

    @Expose
    @Category(name = "config.babyzombieaddons.category.misc", desc = "")
    public MiscConfig misc = new MiscConfig();



    // ── Categories end ──
}
