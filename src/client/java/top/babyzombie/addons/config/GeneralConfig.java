package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;
import top.babyzombie.addons.config.ModConfig.*;
import top.babyzombie.addons.config.hud.HudManager;

public class GeneralConfig {

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.updateChecker", desc = "config.babyzombieaddons.option.updateChecker.desc") @ConfigEditorBoolean @SearchTag("update")
    public boolean updateChecker = true;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.replaceReportWithServerList", desc = "config.babyzombieaddons.option.replaceReportWithServerList.desc") @ConfigEditorBoolean @SearchTag("report")
    public boolean replaceReportWithServerList = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.serverResourcePackAutoAccept", desc = "config.babyzombieaddons.option.serverResourcePackAutoAccept.desc") @ConfigEditorBoolean @SearchTag("resourcepack")
    public boolean serverResourcePackAutoAccept = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.hudEdit", desc = "config.babyzombieaddons.option.hudEdit.desc") @ConfigEditorButton(buttonText = "OPEN")
    public transient Runnable hudEdit = () -> HudManager.openEditScreen(null);
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.playCmd", desc = "config.babyzombieaddons.option.playCmd.desc") @ConfigEditorBoolean @SearchTag("play")
    public boolean playCmd = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.skipSecondPerson", desc = "config.babyzombieaddons.option.skipSecondPerson.desc") @ConfigEditorSlider(minValue = 0, maxValue = 30, minStep = 1) @SearchTag("camera")
    public int skipSecondPerson = 0;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.useTpsAdjustedTime", desc = "config.babyzombieaddons.option.useTpsAdjustedTime.desc") @ConfigEditorBoolean @SearchTag("tps")
    public boolean useTpsAdjustedTime = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.renderPhase", desc = "config.babyzombieaddons.option.renderPhase.desc") @ConfigEditorDropdown
    public WorldRenderPhase renderPhase = WorldRenderPhase.AFTER_ENTITIES;

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.autotip", desc = "") @Accordion
    public Autotip autotip = new Autotip();
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.chat", desc = "") @Accordion
    public Chat chat = new Chat();
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.autoReconnect", desc = "") @Accordion
    public AutoReconnect autoReconnect = new AutoReconnect();
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.autoJoinServer", desc = "") @Accordion
    public AutoJoinServer autoJoinServer = new AutoJoinServer();
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.playerScale", desc = "") @Accordion
    public PlayerScale playerScale = new PlayerScale();
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.handRender", desc = "") @Accordion
    public HandRender handRender = new HandRender();
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.windowTitle", desc = "") @Accordion
    public WindowTitle windowTitle = new WindowTitle();

    public static class Autotip {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autotipEnabled", desc = "config.babyzombieaddons.option.autotipEnabled.desc") @ConfigEditorBoolean
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autotipHideMessages", desc = "config.babyzombieaddons.option.autotipHideMessages.desc") @ConfigEditorBoolean
        public boolean hideMessages = false;
    }

    public static class Chat {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.chatChannelSwitcher", desc = "config.babyzombieaddons.option.chatChannelSwitcher.desc") @ConfigEditorBoolean
        public boolean channelSwitcher = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.chatInContainer", desc = "config.babyzombieaddons.option.chatInContainer.desc") @ConfigEditorBoolean
        public boolean chatInContainer = false;
    }

    public static class AutoReconnect {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoReconnectEnabled", desc = "config.babyzombieaddons.option.autoReconnectEnabled.desc") @ConfigEditorBoolean
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoReconnectDelay", desc = "config.babyzombieaddons.option.autoReconnectDelay.desc") @ConfigEditorSlider(minValue = 1, maxValue = 60, minStep = 1)
        public int delay = 5;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoReconnectMaxRetries", desc = "config.babyzombieaddons.option.autoReconnectMaxRetries.desc") @ConfigEditorSlider(minValue = 0, maxValue = 10, minStep = 1)
        public int maxRetries = 0;
    }

    public static class AutoJoinServer {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoJoinServer", desc = "config.babyzombieaddons.option.autoJoinServer.desc") @ConfigEditorBoolean
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoJoinServerIP", desc = "config.babyzombieaddons.option.autoJoinServerIP.desc") @ConfigEditorText
        public String ip = "";
    }

    public static class PlayerScale {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.playerScaleX", desc = "config.babyzombieaddons.option.playerScaleX.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        public float x = 1.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.playerScaleY", desc = "config.babyzombieaddons.option.playerScaleY.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        public float y = 1.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.playerScaleZ", desc = "config.babyzombieaddons.option.playerScaleZ.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        public float z = 1.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.showCrosshairInThirdPerson", desc = "config.babyzombieaddons.option.showCrosshairInThirdPerson.desc") @ConfigEditorBoolean
        public boolean showCrosshairInThirdPerson = false;
    }

    public static class HandRender {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.disableHandRender", desc = "config.babyzombieaddons.option.disableHandRender.desc") @ConfigEditorBoolean
        public boolean disableAll = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.swapHands", desc = "config.babyzombieaddons.option.swapHands.desc") @ConfigEditorBoolean
        public boolean swapHands = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.customSwingDuration", desc = "config.babyzombieaddons.option.customSwingDuration.desc") @ConfigEditorBoolean
        public boolean customSwingDuration = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.swingDurationTicks", desc = "config.babyzombieaddons.option.swingDurationTicks.desc") @ConfigEditorSlider(minValue = 0, maxValue = 1200, minStep = 1)
        public int swingDurationTicks = 6;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.itemScale", desc = "config.babyzombieaddons.option.itemScale.desc") @ConfigEditorSlider(minValue = 0.1f, maxValue = 1.0f, minStep = 0.05f)
        public float itemScale = 1.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.itemOffsetX", desc = "config.babyzombieaddons.option.itemOffsetX.desc") @ConfigEditorSlider(minValue = -1.0f, maxValue = 1.0f, minStep = 0.05f)
        public float itemOffsetX = 0.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.itemOffsetY", desc = "config.babyzombieaddons.option.itemOffsetY.desc") @ConfigEditorSlider(minValue = -1.0f, maxValue = 1.0f, minStep = 0.05f)
        public float itemOffsetY = 0.0f;
    }

    public static class WindowTitle {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.windowTitleEnabled", desc = "config.babyzombieaddons.option.windowTitleEnabled.desc") @ConfigEditorBoolean
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.windowTitleUpdateInterval", desc = "config.babyzombieaddons.option.windowTitleUpdateInterval.desc") @ConfigEditorSlider(minValue = 1, maxValue = 20, minStep = 1)
        public int updateInterval = 1;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.windowTitleShowLocation", desc = "config.babyzombieaddons.option.windowTitleShowLocation.desc") @ConfigEditorBoolean
        public boolean showLocation = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.windowTitleOverride", desc = "config.babyzombieaddons.option.windowTitleOverride.desc") @ConfigEditorBoolean
        public boolean overrideOriginal = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.windowTitleShowMemory", desc = "config.babyzombieaddons.option.windowTitleShowMemory.desc") @ConfigEditorBoolean
        public boolean showMemory = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.windowTitleShowSystemMemory", desc = "config.babyzombieaddons.option.windowTitleShowSystemMemory.desc") @ConfigEditorBoolean
        public boolean showSystemMemory = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.windowTitleShowPing", desc = "config.babyzombieaddons.option.windowTitleShowPing.desc") @ConfigEditorBoolean
        public boolean showPing = false;
    }
}
