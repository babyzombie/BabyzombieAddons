package top.babyzombie.addons.config;

import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import com.google.gson.annotations.Expose;
import net.minecraft.network.chat.Component;

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

    /** Translates a key via Minecraft's I18n system. */
    private static String t(String key) {
        return Component.translatable(key).getString();
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
