package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.*;
import io.github.notenoughupdates.moulconfig.observer.Property;
import org.lwjgl.glfw.GLFW;

public class MiscConfig {

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.wideMoulConfig", desc = "config.babyzombieaddons.option.wideMoulConfig.desc")
    @ConfigEditorBoolean
    @SearchTag("wide")
    public Property<Boolean> wideMoulConfig = Property.of(false);

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.showClickEventInHover", desc = "config.babyzombieaddons.option.showClickEventInHover.desc")
    @ConfigEditorBoolean
    @SearchTag("hover")
    public boolean showClickEventInHover = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.renderPhase", desc = "config.babyzombieaddons.option.renderPhase.desc")
    @ConfigEditorDropdown
    @SearchTag("render")
    public ModConfig.WorldRenderPhase renderPhase = ModConfig.WorldRenderPhase.AFTER_ENTITIES;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.debugMode", desc = "config.babyzombieaddons.option.debugMode.desc")
    @ConfigEditorBoolean
    @SearchTag("debug")
    public boolean debugMode = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.hypixelModApiDebugLog", desc = "config.babyzombieaddons.option.hypixelModApiDebugLog.desc")
    @ConfigEditorBoolean
    @SearchTag("debug")
    public boolean hypixelModApiDebugLog = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.copyItemInfo", desc = "config.babyzombieaddons.option.copyItemInfo.desc")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    @SearchTag("copy") @SearchTag("key")
    public int copyItemInfo = GLFW.GLFW_KEY_UNKNOWN;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.maxDebugEntities", desc = "config.babyzombieaddons.option.maxDebugEntities.desc")
    @ConfigEditorSlider(minValue = 1, maxValue = 100, minStep = 1)
    @SearchTag("debug")
    public int maxDebugEntities = 20;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.serverVisitExpireMinutes", desc = "config.babyzombieaddons.option.serverVisitExpireMinutes.desc")
    @ConfigEditorSlider(minValue = 1, maxValue = 60, minStep = 1)
    @SearchTag("visit")
    public int serverVisitExpireMinutes = 10;
}
