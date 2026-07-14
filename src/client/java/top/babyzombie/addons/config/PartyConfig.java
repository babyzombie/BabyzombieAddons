package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;

public class PartyConfig {

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.autoAcceptReparty", desc = "config.babyzombieaddons.option.autoAcceptReparty.desc")
    @ConfigEditorBoolean
    @SearchTag("auto")
    public boolean autoAcceptReparty = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.partySelfExecute", desc = "config.babyzombieaddons.option.partySelfExecute.desc")
    @ConfigEditorBoolean @SearchTag("party") @SearchTag("self") @SearchTag("execute")
    public boolean partySelfExecute = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.partyAllinvite", desc = "config.babyzombieaddons.option.partyAllinvite.desc")
    @ConfigEditorBoolean @SearchTag("party") @SearchTag("allinvite")
    public boolean partyAllinvite = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.partyInvite", desc = "config.babyzombieaddons.option.partyInvite.desc")
    @ConfigEditorBoolean @SearchTag("party") @SearchTag("invite")
    public boolean partyInvite = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.partyWarp", desc = "config.babyzombieaddons.option.partyWarp.desc")
    @ConfigEditorBoolean @SearchTag("party") @SearchTag("warp")
    public boolean partyWarp = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.partyWarpDelay", desc = "config.babyzombieaddons.option.partyWarpDelay.desc")
    @ConfigEditorBoolean @SearchTag("party") @SearchTag("warp") @SearchTag("delay")
    public boolean partyWarpDelay = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.partyWarpDelaySeconds", desc = "config.babyzombieaddons.option.partyWarpDelaySeconds.desc")
    @ConfigEditorSlider(minValue = 1, maxValue = 30, minStep = 1) @SearchTag("party") @SearchTag("warp") @SearchTag("seconds")
    public int partyWarpDelaySeconds = 3;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.partyJoinInstance", desc = "config.babyzombieaddons.option.partyJoinInstance.desc")
    @ConfigEditorBoolean @SearchTag("party") @SearchTag("join") @SearchTag("instance")
    public boolean partyJoinInstance = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.partySendCoords", desc = "config.babyzombieaddons.option.partySendCoords.desc")
    @ConfigEditorBoolean @SearchTag("party") @SearchTag("coords") @SearchTag("coordinate")
    public boolean partySendCoords = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.partyPlay", desc = "config.babyzombieaddons.option.partyPlay.desc")
    @ConfigEditorBoolean @SearchTag("party") @SearchTag("play")
    public boolean partyPlay = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.partyStream", desc = "config.babyzombieaddons.option.partyStream.desc")
    @ConfigEditorBoolean @SearchTag("party") @SearchTag("stream")
    public boolean partyStream = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.partyTransfer", desc = "config.babyzombieaddons.option.partyTransfer.desc")
    @ConfigEditorBoolean @SearchTag("party") @SearchTag("transfer")
    public boolean partyTransfer = false;

    @Expose
    @ConfigOption(name = "config.babyzombieaddons.option.dmPartyInvite", desc = "config.babyzombieaddons.option.dmPartyInvite.desc")
    @ConfigEditorBoolean
    @SearchTag("dm")
    public boolean dmPartyInvite = false;
}
