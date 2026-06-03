package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ServerTick;

/**
 * Tracks End Stone Sword / Extreme Focus ability duration.
 */
public final class EndStoneSwordTimer {
    static long time;
    static boolean active;

    private EndStoneSwordTimer() {}

    public static void init() {
        if (!ModConfigManager.get().slayer.endStoneSwordTimer) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("Extreme Focus")) {
                time = ServerTick.getTime();
                active = true;
            }
        });
    }
}
