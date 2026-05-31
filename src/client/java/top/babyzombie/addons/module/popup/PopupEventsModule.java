package top.babyzombie.addons.module.popup;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.util.ChatUtils;

/**
 * On-screen popup notifications for party invites, friend requests,
 * trade requests, and dungeon/kuudra restart prompts.
 */
public final class PopupEventsModule {
    private PopupEventsModule() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = message.getString();

            // Party invite detection
            if (text.contains("has invited you to join their party")) {
                handlePartyInvite(text);
            }
            // Friend request detection
            if (text.contains("has sent you a friend request")) {
                handleFriendRequest(text);
            }
            // Dungeon/kuudra restart
            if (text.contains("Restart") && (text.contains("Dungeon") || text.contains("Kuudra"))) {
                handleRestartPrompt(text);
            }
        });
    }

    private static void handlePartyInvite(String text) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§b[BZA] §eParty invite received!"), true);
        }
    }

    private static void handleFriendRequest(String text) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§b[BZA] §eFriend request received!"), true);
        }
    }

    private static void handleRestartPrompt(String text) {
        // Auto-accept by pressing the restart button could be added here
    }
}
