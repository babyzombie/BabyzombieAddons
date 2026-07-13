package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;
import top.babyzombie.addons.config.ModConfig.*;

public class DungeonConfig {

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.f4CrowdHiding", desc = "config.babyzombieaddons.option.f4CrowdHiding.desc") @ConfigEditorDropdown @SearchTag("f4")
    public CrowdHideMode f4CrowdHiding = CrowdHideMode.OFF;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.deathMessageAction", desc = "config.babyzombieaddons.option.deathMessageAction.desc") @ConfigEditorDropdown @SearchTag("death")
    public DeathMessageAction deathMessageAction = DeathMessageAction.OFF;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.enderPearlRefill", desc = "config.babyzombieaddons.option.enderPearlRefill.desc") @ConfigEditorBoolean @SearchTag("pearl")
    public boolean enderPearlRefill = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoChestClose", desc = "config.babyzombieaddons.option.autoChestClose.desc") @ConfigEditorBoolean @SearchTag("chest")
    public boolean autoChestClose = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.muteStormThunder", desc = "config.babyzombieaddons.option.muteStormThunder.desc") @ConfigEditorBoolean @SearchTag("storm")
    public boolean muteStormThunder = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.muteRareDropSound", desc = "config.babyzombieaddons.option.muteRareDropSound.desc") @ConfigEditorBoolean @SearchTag("rare")
    public boolean muteRareDropSound = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoOpenPotions", desc = "config.babyzombieaddons.option.autoOpenPotions.desc") @ConfigEditorDropdown @SearchTag("potion")
    public AutoPotionsMode autoOpenPotions = AutoPotionsMode.OFF;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.dailyRunsCounter", desc = "config.babyzombieaddons.option.dailyRunsCounter.desc") @ConfigEditorDropdown @SearchTag("daily")
    public DailyCounterMode dailyRunsCounter = DailyCounterMode.OFF;

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.requeue", desc = "") @Accordion
    public Requeue requeue = new Requeue();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.witherCloak", desc = "") @Accordion
    public WitherCloak witherCloak = new WitherCloak();

    public static class Requeue {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.dungeonRequeue", desc = "config.babyzombieaddons.option.dungeonRequeue.desc") @ConfigEditorDropdown @SearchTag("requeue")
        public RequeueMode dungeonRequeue = RequeueMode.OFF;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.dungeonRequeueDelay", desc = "config.babyzombieaddons.option.dungeonRequeueDelay.desc") @ConfigEditorSlider(minValue = 0, maxValue = 60, minStep = 1)
        public int dungeonRequeueDelay = 0;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraRequeue", desc = "config.babyzombieaddons.option.kuudraRequeue.desc") @ConfigEditorDropdown @SearchTag("kuudra")
        public RequeueMode kuudraRequeue = RequeueMode.OFF;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.kuudraRequeueDelay", desc = "config.babyzombieaddons.option.kuudraRequeueDelay.desc") @ConfigEditorSlider(minValue = 0, maxValue = 60, minStep = 1)
        public int kuudraRequeueDelay = 0;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.requeueMessage", desc = "config.babyzombieaddons.option.requeueMessage.desc") @ConfigEditorText
        public String requeueMessage = "going in %delay%";
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.requeueCancelMessage", desc = "config.babyzombieaddons.option.requeueCancelMessage.desc") @ConfigEditorText
        public String requeueCancelMessage = "ok";
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.requeueCancelKeywords", desc = "config.babyzombieaddons.option.requeueCancelKeywords.desc") @ConfigEditorText
        public String requeueCancelKeywords = "c|cancel|n|nr|wait|stop|dt|don't|gtg|tyfr|tyfrs|gtg tyfr|gtg tyfrs|no key|别急|等会|等下|先别开";
    }

    public static class WitherCloak {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.witherCloakTimer", desc = "config.babyzombieaddons.option.witherCloakTimer.desc") @ConfigEditorBoolean @SearchTag("wither")
        public boolean witherCloakTimer = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.soulwardTimer", desc = "config.babyzombieaddons.option.soulwardTimer.desc") @ConfigEditorBoolean @SearchTag("soulward")
        public boolean soulwardTimer = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.alignedTimer", desc = "config.babyzombieaddons.option.alignedTimer.desc") @ConfigEditorBoolean
        public boolean alignedTimer = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.gravityStormTimer", desc = "config.babyzombieaddons.option.gravityStormTimer.desc") @ConfigEditorBoolean @SearchTag("gravity")
        public boolean gravityStormTimer = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideChargedCreepers", desc = "config.babyzombieaddons.option.hideChargedCreepers.desc") @ConfigEditorBoolean @SearchTag("creeper")
        public boolean hideChargedCreepers = false;
    }
}
