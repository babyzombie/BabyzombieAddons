package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ServerTick;

/**
 * Tracks Reaper Armor ability activation.
 */
public final class ReaperArmorTimer {
    static long time;

    private ReaperArmorTimer() {}

    public static void init() {
        if (!ModConfigManager.get().slayer.reaperArmorTimer) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("Reaper")) {
                time = ServerTick.getTime();
            }
        });
    }
}
