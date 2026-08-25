package top.babyzombie.addons.module.chat;

import top.babyzombie.addons.module.chat.containerchat.ContainerChatModule;
import top.babyzombie.addons.module.chat.playcmd.PlayCmdModule;

public class ChatModule {
    private ChatModule() {}

    public static void init() {
        PartyModule.init();
        PlayCmdModule.init();
        AutotipModule.init();
        ChatChannelModule.init();
        WaypointMarkerModule.init();
        ContainerChatModule.init();
        PopupEventsModule.init();
    }
}
