package top.babyzombie.addons.config;

import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.Social;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import com.google.gson.annotations.Expose;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.net.URI;
import java.util.List;
import net.minecraft.util.Util;

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
    public enum ChestCounterMode {
        KUUDRA_ONLY, INCLUDE_CRIMSON_DUNGEON, EVERYWHERE;
        @Override public String toString() { return t("config.babyzombieaddons.option.chestCounterMode." + name()); }
    }
    public enum MineshaftWarpMode {
        OFF, TITLE_ONLY, TITLE_AND_SOUND, SEND_PTME, PTME_AND_WARP;
        @Override public String toString() { return t("config.babyzombieaddons.option.glaciteMineshaftWarp." + name()); }
    }
    public enum GlaciteMineshaftPortalAction {
        NONE, SEND_PTME, PTME_AND_WARP;
        @Override public String toString() { return t("config.babyzombieaddons.option.glaciteMineshaftPortalAction." + name()); }
    }
    public enum GlaciteMineshaftPortalSound {
        BELL(SoundEvents.BELL_BLOCK),
        NOTE_BLOCK(SoundEvents.NOTE_BLOCK_PLING.value()),
        EXPERIENCE(SoundEvents.EXPERIENCE_ORB_PICKUP),
        LEVEL_UP(SoundEvents.PLAYER_LEVELUP),
        DRAGON(SoundEvents.ENDER_DRAGON_GROWL),
        ANVIL(SoundEvents.ANVIL_LAND),
        GOAT_HORN(SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(2).value()),
        LAVA_CHICKEN(SoundEvents.MUSIC_DISC_LAVA_CHICKEN.value());

        public final SoundEvent sound;
        GlaciteMineshaftPortalSound(SoundEvent sound) { this.sound = sound; }
        @Override public String toString() { return t("config.babyzombieaddons.option.glaciteMineshaftPortalSoundSelect." + name()); }
    }
    public enum MineshaftCorpseRenderMode {
        FILLED, WIREFRAME;
        @Override public String toString() { return t("config.babyzombieaddons.option.mineshaftCorpseRenderMode." + name()); }
    }
    public enum GummyPolarBearMode {
        OFF, SMOLDERING_TOMB_ONLY, EVERYWHERE_EXCEPT_DUNGEON;
        @Override public String toString() { return t("config.babyzombieaddons.option.reheatedGummyPolarBear." + name()); }
    }
    public enum MarkerChannel {
        DEFAULT, AC, PC, GC;
        @Override public String toString() { return t("config.babyzombieaddons.option.markerChannel." + name()); }
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
    public enum PerkShopItem {
        CANNONEER_ROUTE("Cannoneer Route"), CROWD_CONTROL_ROUTE("Crowd Control Route"),
        SPECIALIST_ROUTE("Specialist Route"), SUPPORT_ROUTE("Support Route"),
        ACCELERATED_SHOP("Accelerated Shop"), BLAST_RADIUS("Blast Radius"),
        CANNON_PROFICIENCY("Cannon Proficiency"), MULTI_SHOP("Multi-Shop"),
        RAPID_FIRE("Rapid Fire"), STEADY_AIM("Steady Aim"),
        SWEEPING_EDGE("Sweeping Edge"), FREEZING_TOUCH("Freezing Touch"),
        PROTECTIVE_AURA("Protective Aura"), MINIATURE_NUKE("Miniature Nuke"),
        BONUS_DAMAGE("Bonus Damage"), BLIGHT_SLAYER("Blight Slayer"),
        STEADY_HANDS("Steady Hands"), BALLISTA_MECHANIC("Ballista Mechanic"),
        BOMBERMAN("Bomberman"), KUUDRA_SLAYER("Kuudra Slayer"),
        MINING_FRENZY("Mining Frenzy"), HEALING_AURA("Healing Aura"),
        MANA_AURA("Mana Aura"), FASTER_RESPAWN("Faster Respawn"),
        REVIVE_FINAL_KILLED("Revive Final Killed"), REVIVE_DEAD("Revive Dead"),
        AUTO_REVIVE("Auto Revive"), HUMAN_CANNONBALL("Human Cannonball"),
        REMOTE_PERK_SHOP("Remote Perk Shop"), ELLES_LAVA_ROD("Elle's Lava Rod"),
        ELLES_PICKAXE("Elle's Pickaxe"), FILL_YOUR_QUIVER("Fill your Quiver");

        private final String displayName;
        PerkShopItem(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
        @Override public String toString() { return displayName; }
    }
    public enum MusicDisc {
        DISC_5(178), DISC_11(71), DISC_13(178), BLOCKS(345), CAT(185), CHIRP(185), FAR(174),
        LAVA_CHICKEN(135), MALL(197), MELLOHI(96), PIGSTEP(148), STAL(150), STRAD(188),
        WAIT(237), WARD(251), OTHERSIDE(195), RELIC(219), CREATOR(176),
        CREATOR_MUSIC_BOX(73), PRECIPICE(299), TEARS(175),

        // ── 自定义唱片槽位（取决于 config 目录下的 .ogg 文件）──
        CUSTOM_1(0), CUSTOM_2(0), CUSTOM_3(0), CUSTOM_4(0), CUSTOM_5(0),
        CUSTOM_6(0), CUSTOM_7(0), CUSTOM_8(0), CUSTOM_9(0);

        private final int durationSeconds;

        MusicDisc(int durationSeconds) {
            this.durationSeconds = durationSeconds;
        }

        /** 唱片时长（秒） */
        public int getDurationSeconds() {
            if (isCustom() && isCustomActive()) {
                var info = top.babyzombie.addons.module.dungeon.CustomDiscScanner.getInfo(this);
                if (info != null) return info.durationSeconds();
            }
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
                case CUSTOM_1, CUSTOM_2, CUSTOM_3, CUSTOM_4, CUSTOM_5,
                     CUSTOM_6, CUSTOM_7, CUSTOM_8, CUSTOM_9 -> {
                    var info = top.babyzombie.addons.module.dungeon.CustomDiscScanner.getInfo(this);
                    if (info != null) yield "§b" + info.displayName();
                    yield "§7" + t("config.babyzombieaddons.option.musicDisc.notInstalled");
                }
            };
        }

        public Identifier getSoundId() {
            if (isCustom()) {
                int n = ordinal() - CUSTOM_1.ordinal() + 1;
                return Identifier.fromNamespaceAndPath("babyzombieaddons", "custom_disc_" + n);
            }
            String n = name().toLowerCase().replace("disc_", "");
            return Identifier.fromNamespaceAndPath("minecraft", "music_disc." + n);
        }

        private boolean isCustom() {
            return ordinal() >= CUSTOM_1.ordinal();
        }

        public boolean isCustomActive() {
            return isCustom() && top.babyzombie.addons.module.dungeon.CustomDiscScanner.getInfo(this) != null;
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
                linkSocial(
                        StructuredText.translatable("config.babyzombieaddons.social.modrinth"),
                        new MyResourceLocation("babyzombieaddons", "textures/modrinth.png"),
                        "https://modrinth.com/mod/babyzombieaddons"
                ),
                linkSocial(
                        StructuredText.translatable("config.babyzombieaddons.social.github"),
                        new MyResourceLocation("babyzombieaddons", "textures/github.png"),
                        "https://github.com/babyzombie/BabyzombieAddons"
                ),
                linkSocial(
                        StructuredText.translatable("config.babyzombieaddons.social.gitee"),
                        new MyResourceLocation("babyzombieaddons", "textures/gitee.png"),
                        "https://gitee.com/Bluesky-kk/BabyzombieAddons"
                )
        );
    }

    /**
     * 点击直接打开链接：走 MC 官方 {@link Util#getPlatform()} 的 openUri
     * （Windows 上为 ShellExecute 进程调用），比 MoulConfig 默认的
     * AWT {@link java.awt.Desktop#browse} 更可靠——后者在部分 Windows 环境下
     * 会抛异常而退化成"聊天栏点击链接"。
     */
    private static Social linkSocial(StructuredText name, MyResourceLocation icon, String url) {
        return new Social() {
            @Override
            public void onClick() {
                Util.getPlatform().openUri(URI.create(url));
            }

            @Override
            public List<StructuredText> getTooltip() {
                return List.of(name);
            }

            @Override
            public MyResourceLocation getIcon() {
                return icon;
            }
        };
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
    @Category(name = "config.babyzombieaddons.category.minigames", desc = "")
    public MinigamesConfig minigames = new MinigamesConfig();

    @Expose
    @Category(name = "config.babyzombieaddons.category.misc", desc = "")
    public MiscConfig misc = new MiscConfig();



    // ── Categories end ──
}
