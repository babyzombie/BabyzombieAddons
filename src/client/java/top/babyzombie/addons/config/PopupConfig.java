package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;
import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.module.popup.PopupEventsModule.PopupSound;

public class PopupConfig {

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.popupYes", desc = "config.babyzombieaddons.option.popupYes.desc")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_Y)
    @SearchTag("popup") @SearchTag("key")
    public int popupYes = GLFW.GLFW_KEY_Y;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.popupNo", desc = "config.babyzombieaddons.option.popupNo.desc")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_N)
    @SearchTag("popup") @SearchTag("key")
    public int popupNo = GLFW.GLFW_KEY_N;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.popupSound", desc = "config.babyzombieaddons.option.popupSound.desc")
    @ConfigEditorDropdown
    public PopupSound popupSound = PopupSound.BELL;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.popupPartyInvite", desc = "config.babyzombieaddons.option.popupPartyInvite.desc")
    @ConfigEditorBoolean
    @SearchTag("party")
    public boolean popupPartyInvite = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.popupGuildPartyInvite", desc = "config.babyzombieaddons.option.popupGuildPartyInvite.desc")
    @ConfigEditorBoolean
    @SearchTag("party")
    public boolean popupGuildPartyInvite = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.popupFriendRequest", desc = "config.babyzombieaddons.option.popupFriendRequest.desc")
    @ConfigEditorBoolean
    @SearchTag("friend")
    public boolean popupFriendRequest = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.popupDuelsRequest", desc = "config.babyzombieaddons.option.popupDuelsRequest.desc")
    @ConfigEditorBoolean
    @SearchTag("duels")
    public boolean popupDuelsRequest = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.popupSkyblockTrade", desc = "config.babyzombieaddons.option.popupSkyblockTrade.desc")
    @ConfigEditorBoolean
    @SearchTag("trade")
    public boolean popupSkyblockTrade = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.popupDungeonRestart", desc = "config.babyzombieaddons.option.popupDungeonRestart.desc")
    @ConfigEditorBoolean
    @SearchTag("dungeon")
    public boolean popupDungeonRestart = false;

    // popupBaitLow moved to FishingConfig
}
