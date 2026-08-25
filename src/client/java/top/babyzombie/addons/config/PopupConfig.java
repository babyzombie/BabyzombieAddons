package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;
import com.mojang.blaze3d.platform.InputConstants;
import top.babyzombie.addons.module.chat.PopupEventsModule.PopupSound;

public class PopupConfig {

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.popupYes", desc = "config.babyzombieaddons.option.popupYes.desc")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_Y)
    @SearchTag("popup") @SearchTag("key")
    public int popupYes = InputConstants.KEY_Y;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.popupNo", desc = "config.babyzombieaddons.option.popupNo.desc")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_N)
    @SearchTag("popup") @SearchTag("key")
    public int popupNo = InputConstants.KEY_N;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.popupSound", desc = "config.babyzombieaddons.option.popupSound.desc")
    @ConfigEditorDropdown @SearchTag("popup") @SearchTag("sound")
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
