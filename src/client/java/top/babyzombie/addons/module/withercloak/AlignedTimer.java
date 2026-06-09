package top.babyzombie.addons.module.withercloak;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

/**
 * Tracks Cells Alignment duration and source player.
 */
public final class AlignedTimer {
    static long time;
    static String by = "";

    private AlignedTimer() {}

    public static void init() {
        // Self-alignment via chat
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !ModConfigManager.get().witherCloak.alignedTimer) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            String text = ChatUtils.stripColor(message.getString());

            if (text.matches("You aligned (yourself|\\d+ other players?)!")) {
                time = ServerTick.getTime();
                by = "§e§lby §r§ayourself";
            }
        });

        // Aligned by another player
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !ModConfigManager.get().witherCloak.alignedTimer) return;
            String text = message.getString();
            if (text.contains("casted Cells Alignment on you")) {
                String player = text.split(" ")[0];
                if (!player.contains(" ")) {
                    time = ServerTick.getTime();
                    by = "§e§lby §r§b" + player;
                }
            }
        });
    }
}
