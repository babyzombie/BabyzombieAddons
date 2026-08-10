package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import top.babyzombie.addons.config.hud.HudManager;

import java.util.ArrayList;
import java.util.List;

public class GeneralConfig {

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.updateChecker", desc = "config.babyzombieaddons.option.updateChecker.desc") @ConfigEditorBoolean @SearchTag("update")
    public boolean updateChecker = true;
    @ConfigOption(name = "config.babyzombieaddons.option.hudEdit", desc = "config.babyzombieaddons.option.hudEdit.desc") @ConfigEditorButton(buttonText = "OPEN") @SearchTag("hud")
    public transient Runnable hudEdit = () -> HudManager.openEditScreen(Minecraft.getInstance().gui.screen());
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.serverResourcePackAutoAccept", desc = "config.babyzombieaddons.option.serverResourcePackAutoAccept.desc") @ConfigEditorBoolean @SearchTag("resourcepack")
    public boolean serverResourcePackAutoAccept = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.cancelKeyRelease", desc = "config.babyzombieaddons.option.cancelKeyRelease.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_LALT) @SearchTag("key")
    public int cancelKeyRelease = InputConstants.KEY_LALT;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.playCmd", desc = "config.babyzombieaddons.option.playCmd.desc") @ConfigEditorBoolean @SearchTag("play")
    public boolean playCmd = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.skipSecondPerson", desc = "config.babyzombieaddons.option.skipSecondPerson.desc") @ConfigEditorSlider(minValue = 0, maxValue = 30, minStep = 1) @SearchTag("camera")
    public int skipSecondPerson = 0;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.secondPerson", desc = "config.babyzombieaddons.option.secondPerson.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("camera") @SearchTag("key")
    public int secondPerson = -1;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.useTpsAdjustedTime", desc = "config.babyzombieaddons.option.useTpsAdjustedTime.desc") @ConfigEditorBoolean @SearchTag("tps")
    public boolean useTpsAdjustedTime = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.pauseScreen", desc = "") @Accordion
    public PauseScreen pauseScreen = new PauseScreen();
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.titleScreen", desc = "") @Accordion
    public TitleScreen titleScreen = new TitleScreen();
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
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.waypointMarker", desc = "") @Accordion
    public WaypointMarker waypointMarker = new WaypointMarker();
    @Expose @ConfigOption(name = "config.babyzombieaddons.group.entityHider", desc = "") @Accordion
    public EntityHider entityHider = new EntityHider();

    public static class WaypointMarker {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.markerKey", desc = "config.babyzombieaddons.option.markerKey.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("marker") @SearchTag("key")
        public int markerKey = -1;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.markerChannel", desc = "config.babyzombieaddons.option.markerChannel.desc") @ConfigEditorDropdown @SearchTag("marker") @SearchTag("channel")
        public ModConfig.MarkerChannel markerChannel = ModConfig.MarkerChannel.PC;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.markerSuffix", desc = "config.babyzombieaddons.option.markerSuffix.desc") @ConfigEditorText @SearchTag("marker") @SearchTag("suffix")
        public String markerSuffix = "";
    }

    public static class EntityHider {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.entityHiderEnabled", desc = "config.babyzombieaddons.option.entityHiderEnabled.desc") @ConfigEditorBoolean @SearchTag("hide") @SearchTag("entity")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.entityHiderToggleKey", desc = "config.babyzombieaddons.option.entityHiderToggleKey.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN) @SearchTag("hide") @SearchTag("key")
        public int toggleKey = GLFW.GLFW_KEY_UNKNOWN;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.entityHiderSkyblockOnly", desc = "config.babyzombieaddons.option.entityHiderSkyblockOnly.desc") @ConfigEditorBoolean @SearchTag("hide") @SearchTag("skyblock")
        public boolean skyblockOnly = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.entityHiderHideOwnBobber", desc = "config.babyzombieaddons.option.entityHiderHideOwnBobber.desc") @ConfigEditorBoolean @SearchTag("hide") @SearchTag("bobber") @SearchTag("fishing")
        public boolean hideOwnBobber = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.entityHiderHideOwnMount", desc = "config.babyzombieaddons.option.entityHiderHideOwnMount.desc") @ConfigEditorBoolean @SearchTag("hide") @SearchTag("mount") @SearchTag("riding")
        public boolean hideOwnMount = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.entityHiderRange", desc = "config.babyzombieaddons.option.entityHiderRange.desc") @ConfigEditorSlider(minValue = 1, maxValue = 64, minStep = 1) @SearchTag("hide") @SearchTag("range") @SearchTag("distance")
        public int range = 5;
    }

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
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.toggleHandRenderKey", desc = "config.babyzombieaddons.option.toggleHandRenderKey.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("hand") @SearchTag("key")
        public int toggleHandRenderKey = -1;
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
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.windowTitlePingRangeSeconds", desc = "config.babyzombieaddons.option.windowTitlePingRangeSeconds.desc") @ConfigEditorSlider(minValue = 0, maxValue = 60, minStep = 1) @SearchTag("title") @SearchTag("ping")
        public int pingRangeSeconds = 5;
    }

    public static class TitleScreen {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideFriendsButton", desc = "config.babyzombieaddons.option.hideFriendsButton.desc") @ConfigEditorBoolean @SearchTag("title") @SearchTag("hide")
        public boolean hideFriends = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideLanguageButton", desc = "config.babyzombieaddons.option.hideLanguageButton.desc") @ConfigEditorBoolean @SearchTag("title") @SearchTag("hide") @SearchTag("language")
        public boolean hideLanguage = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideAccessibilityButton", desc = "config.babyzombieaddons.option.hideAccessibilityButton.desc") @ConfigEditorBoolean @SearchTag("title") @SearchTag("hide") @SearchTag("accessibility")
        public boolean hideAccessibility = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.titleQuickButtons", desc = "config.babyzombieaddons.option.titleQuickButtons.desc") @ConfigEditorBoolean @SearchTag("title") @SearchTag("quick")
        public boolean enableQuickButtons = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.titleQuickButtonOrder", desc = "config.babyzombieaddons.option.titleQuickButtonOrder.desc") @ConfigEditorDraggableList @SearchTag("title") @SearchTag("quick")
        public List<QuickButtonType> quickButtonOrder = new ArrayList<>();
    }

    public static class PauseScreen {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.confirmWindowClose", desc = "config.babyzombieaddons.option.confirmWindowClose.desc") @ConfigEditorBoolean @SearchTag("pause") @SearchTag("close") @SearchTag("exit")
        public boolean confirmWindowClose = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.confirmDisconnect", desc = "config.babyzombieaddons.option.confirmDisconnect.desc") @ConfigEditorBoolean @SearchTag("pause") @SearchTag("disconnect")
        public boolean confirmDisconnect = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideReturnToGame", desc = "config.babyzombieaddons.option.hideReturnToGame.desc") @ConfigEditorBoolean @SearchTag("pause")
        public boolean hideReturnToGame = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideIconButtonRow", desc = "config.babyzombieaddons.option.hideIconButtonRow.desc") @ConfigEditorBoolean @SearchTag("pause")
        public boolean hideIconButtonRow = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.group.hideButtons", desc = "") @Accordion
        public HideButtons hideButtons = new HideButtons();

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.enableQuickButtons", desc = "config.babyzombieaddons.option.enableQuickButtons.desc") @ConfigEditorBoolean @SearchTag("pause")
        public boolean enableQuickButtons = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.quickButtonOrder", desc = "config.babyzombieaddons.option.quickButtonOrder.desc") @ConfigEditorDraggableList @SearchTag("pause")
        public List<QuickButtonType> quickButtonOrder = new ArrayList<>();
    }

    public static class HideButtons {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideAdvancements", desc = "config.babyzombieaddons.option.hideAdvancements.desc") @ConfigEditorBoolean @SearchTag("pause")
        public boolean hideAdvancements = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideStats", desc = "config.babyzombieaddons.option.hideStats.desc") @ConfigEditorBoolean @SearchTag("pause")
        public boolean hideStats = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideReportBugs", desc = "config.babyzombieaddons.option.hideReportBugs.desc") @ConfigEditorBoolean @SearchTag("pause")
        public boolean hideReportBugs = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideFeedback", desc = "config.babyzombieaddons.option.hideFeedback.desc") @ConfigEditorBoolean @SearchTag("pause")
        public boolean hideFeedback = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideFriends", desc = "config.babyzombieaddons.option.hideFriends.desc") @ConfigEditorBoolean @SearchTag("pause")
        public boolean hideFriends = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hidePlayerReporting", desc = "config.babyzombieaddons.option.hidePlayerReporting.desc") @ConfigEditorBoolean @SearchTag("pause")
        public boolean hidePlayerReporting = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideCustomAdditions", desc = "config.babyzombieaddons.option.hideCustomAdditions.desc") @ConfigEditorBoolean @SearchTag("pause")
        public boolean hideCustomAdditions = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideOptions", desc = "config.babyzombieaddons.option.hideOptions.desc") @ConfigEditorBoolean @SearchTag("pause")
        public boolean hideOptions = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideMultiplayerOptions", desc = "config.babyzombieaddons.option.hideMultiplayerOptions.desc") @ConfigEditorBoolean @SearchTag("pause")
        public boolean hideMultiplayerOptions = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideDisconnect", desc = "config.babyzombieaddons.option.hideDisconnect.desc") @ConfigEditorBoolean @SearchTag("pause")
        public boolean hideDisconnect = false;
    }

    public enum QuickButtonType {
        SINGLEPLAYER,
        SERVER_LIST,
        VIDEO_SETTINGS,
        KEY_BINDS,
        SOUND_OPTIONS,
        BZA_CONFIG,
        HYPIXEL,
        SKYBLOCKER,
        FIRMAMENT,
        SKYHANNI,
        AARON;

        /** 第三方 mod 设置按钮对应的 mod id；非第三方类型返回 null */
        public String modId() {
            return switch (this) {
                case SKYBLOCKER -> "skyblocker";
                case FIRMAMENT -> "firmament";
                case SKYHANNI -> "skyhanni";
                case AARON -> "aaron-mod";
                default -> null;
            };
        }

        /** 第三方 mod 设置按钮：对应 mod（及 ModMenu）未安装时不可用，渲染时跳过 */
        public boolean isAvailable() {
            var id = modId();
            if (id == null) return true;
            return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(id)
                    && net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("modmenu");
        }

        @Override
        public String toString() {
            return net.minecraft.network.chat.Component.translatable(
                    "config.babyzombieaddons.quickbutton." + name()).getString();
        }
    }
}
