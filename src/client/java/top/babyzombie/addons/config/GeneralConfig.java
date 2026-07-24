package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.config.hud.HudManager;

public class GeneralConfig {

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.updateChecker", desc = "config.babyzombieaddons.option.updateChecker.desc") @ConfigEditorBoolean @SearchTag("update")
    public boolean updateChecker = true;
    @ConfigOption(name = "config.babyzombieaddons.option.hudEdit", desc = "config.babyzombieaddons.option.hudEdit.desc") @ConfigEditorButton(buttonText = "OPEN") @SearchTag("hud")
    public transient Runnable hudEdit = () -> HudManager.openEditScreen(Minecraft.getInstance().gui.screen());
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.replaceReportWithServerList", desc = "config.babyzombieaddons.option.replaceReportWithServerList.desc") @ConfigEditorBoolean @SearchTag("report")
    public boolean replaceReportWithServerList = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.serverResourcePackAutoAccept", desc = "config.babyzombieaddons.option.serverResourcePackAutoAccept.desc") @ConfigEditorBoolean @SearchTag("resourcepack")
    public boolean serverResourcePackAutoAccept = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.cancelKeyRelease", desc = "config.babyzombieaddons.option.cancelKeyRelease.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_LEFT_ALT) @SearchTag("key")
    public int cancelKeyRelease = GLFW.GLFW_KEY_LEFT_ALT;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.playCmd", desc = "config.babyzombieaddons.option.playCmd.desc") @ConfigEditorBoolean @SearchTag("play")
    public boolean playCmd = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.skipSecondPerson", desc = "config.babyzombieaddons.option.skipSecondPerson.desc") @ConfigEditorSlider(minValue = 0, maxValue = 30, minStep = 1) @SearchTag("camera")
    public int skipSecondPerson = 0;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.secondPerson", desc = "config.babyzombieaddons.option.secondPerson.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN) @SearchTag("camera") @SearchTag("key")
    public int secondPerson = GLFW.GLFW_KEY_UNKNOWN;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.useTpsAdjustedTime", desc = "config.babyzombieaddons.option.useTpsAdjustedTime.desc") @ConfigEditorBoolean @SearchTag("tps")
    public boolean useTpsAdjustedTime = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.autotip", desc = "") @Accordion
    public Autotip autotip = new Autotip();
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.chat", desc = "") @Accordion
    public Chat chat = new Chat();
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.autoReconnect", desc = "") @Accordion
    public AutoReconnect autoReconnect = new AutoReconnect();
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.autoJoinServer", desc = "") @Accordion
    public AutoJoinServer autoJoinServer = new AutoJoinServer();
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.selfPlayerRender", desc = "") @Accordion
    public SelfPlayerRender selfPlayerRender = new SelfPlayerRender();
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.handRender", desc = "") @Accordion
    public HandRender handRender = new HandRender();
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.windowTitle", desc = "") @Accordion
    public WindowTitle windowTitle = new WindowTitle();

    public static class Autotip {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autotipEnabled", desc = "config.babyzombieaddons.option.autotipEnabled.desc") @ConfigEditorBoolean @SearchTag("autotip")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autotipHideMessages", desc = "config.babyzombieaddons.option.autotipHideMessages.desc") @ConfigEditorBoolean @SearchTag("autotip")
        public boolean hideMessages = false;
    }

    public static class Chat {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.chatChannelSwitcher", desc = "config.babyzombieaddons.option.chatChannelSwitcher.desc") @ConfigEditorBoolean @SearchTag("chat")
        public boolean channelSwitcher = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.chatInContainer", desc = "config.babyzombieaddons.option.chatInContainer.desc") @ConfigEditorBoolean @SearchTag("chat")
        public boolean chatInContainer = false;
    }

    public static class AutoReconnect {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoReconnectEnabled", desc = "config.babyzombieaddons.option.autoReconnectEnabled.desc") @ConfigEditorBoolean @SearchTag("reconnect")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoReconnectDelay", desc = "config.babyzombieaddons.option.autoReconnectDelay.desc") @ConfigEditorSlider(minValue = 1, maxValue = 60, minStep = 1) @SearchTag("reconnect") @SearchTag("delay")
        public int delay = 5;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoReconnectMaxRetries", desc = "config.babyzombieaddons.option.autoReconnectMaxRetries.desc") @ConfigEditorSlider(minValue = 0, maxValue = 10, minStep = 1) @SearchTag("reconnect") @SearchTag("retry")
        public int maxRetries = 0;
    }

    public static class AutoJoinServer {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoJoinServer", desc = "config.babyzombieaddons.option.autoJoinServer.desc") @ConfigEditorBoolean @SearchTag("join") @SearchTag("server")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoJoinServerIP", desc = "config.babyzombieaddons.option.autoJoinServerIP.desc") @ConfigEditorText @SearchTag("join") @SearchTag("server") @SearchTag("ip")
        public String ip = "";
    }

    public static class SelfPlayerRender {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.playerScaleX", desc = "config.babyzombieaddons.option.playerScaleX.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 2.0f, minStep = 0.01f) @SearchTag("scale") @SearchTag("player")
        public float x = 1.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.playerScaleY", desc = "config.babyzombieaddons.option.playerScaleY.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 2.0f, minStep = 0.01f) @SearchTag("scale") @SearchTag("player")
        public float y = 1.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.playerScaleZ", desc = "config.babyzombieaddons.option.playerScaleZ.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 2.0f, minStep = 0.01f) @SearchTag("scale") @SearchTag("player")
        public float z = 1.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.playerAlpha", desc = "config.babyzombieaddons.option.playerAlpha.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.01f) @SearchTag("alpha") @SearchTag("transparency") @SearchTag("player")
        public float alpha = 1.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.showCrosshairInThirdPerson", desc = "config.babyzombieaddons.option.showCrosshairInThirdPerson.desc") @ConfigEditorBoolean @SearchTag("crosshair") @SearchTag("thirdperson")
        public boolean showCrosshairInThirdPerson = false;
    }

    public static class HandRender {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.disableHandRender", desc = "config.babyzombieaddons.option.disableHandRender.desc") @ConfigEditorBoolean @SearchTag("hand")
        public boolean disableAll = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.swapHands", desc = "config.babyzombieaddons.option.swapHands.desc") @ConfigEditorBoolean @SearchTag("hand") @SearchTag("swap")
        public boolean swapHands = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.toggleHandRenderKey", desc = "config.babyzombieaddons.option.toggleHandRenderKey.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN) @SearchTag("hand") @SearchTag("key")
        public int toggleHandRenderKey = GLFW.GLFW_KEY_UNKNOWN;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.customSwingDuration", desc = "config.babyzombieaddons.option.customSwingDuration.desc") @ConfigEditorBoolean @SearchTag("hand") @SearchTag("swing")
        public boolean customSwingDuration = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.swingDurationTicks", desc = "config.babyzombieaddons.option.swingDurationTicks.desc") @ConfigEditorSlider(minValue = 0, maxValue = 1200, minStep = 1) @SearchTag("hand") @SearchTag("swing") @SearchTag("duration")
        public int swingDurationTicks = 6;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.itemScale", desc = "config.babyzombieaddons.option.itemScale.desc") @ConfigEditorSlider(minValue = 0.1f, maxValue = 1.0f, minStep = 0.01f) @SearchTag("hand") @SearchTag("item") @SearchTag("scale")
        public float itemScale = 1.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.handAlpha", desc = "config.babyzombieaddons.option.handAlpha.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.01f) @SearchTag("hand") @SearchTag("alpha") @SearchTag("transparency")
        public float alpha = 1.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.itemOffsetX", desc = "config.babyzombieaddons.option.itemOffsetX.desc") @ConfigEditorSlider(minValue = -1.0f, maxValue = 1.0f, minStep = 0.01f) @SearchTag("hand") @SearchTag("item") @SearchTag("offset")
        public float itemOffsetX = 0.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.itemOffsetY", desc = "config.babyzombieaddons.option.itemOffsetY.desc") @ConfigEditorSlider(minValue = -1.0f, maxValue = 1.0f, minStep = 0.01f) @SearchTag("hand") @SearchTag("item") @SearchTag("offset")
        public float itemOffsetY = 0.0f;
    }

    public static class WindowTitle {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.windowTitleEnabled", desc = "config.babyzombieaddons.option.windowTitleEnabled.desc") @ConfigEditorBoolean @SearchTag("title") @SearchTag("window")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.windowTitleUpdateInterval", desc = "config.babyzombieaddons.option.windowTitleUpdateInterval.desc") @ConfigEditorSlider(minValue = 1, maxValue = 20, minStep = 1) @SearchTag("title") @SearchTag("interval")
        public int updateInterval = 1;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.windowTitleShowLocation", desc = "config.babyzombieaddons.option.windowTitleShowLocation.desc") @ConfigEditorBoolean @SearchTag("title") @SearchTag("location")
        public boolean showLocation = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.windowTitleOverride", desc = "config.babyzombieaddons.option.windowTitleOverride.desc") @ConfigEditorBoolean @SearchTag("title") @SearchTag("override")
        public boolean overrideOriginal = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.windowTitleShowMemory", desc = "config.babyzombieaddons.option.windowTitleShowMemory.desc") @ConfigEditorBoolean @SearchTag("title") @SearchTag("memory")
        public boolean showMemory = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.windowTitleShowSystemMemory", desc = "config.babyzombieaddons.option.windowTitleShowSystemMemory.desc") @ConfigEditorBoolean @SearchTag("title") @SearchTag("memory")
        public boolean showSystemMemory = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.windowTitleShowPing", desc = "config.babyzombieaddons.option.windowTitleShowPing.desc") @ConfigEditorBoolean @SearchTag("title") @SearchTag("ping")
        public boolean showPing = false;
    }
}
