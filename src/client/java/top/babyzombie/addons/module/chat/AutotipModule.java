package top.babyzombie.addons.module.chat;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.regex.Pattern;

public final class AutotipModule {
    private static final int TIP_INTERVAL_TICKS = 24600; // 20.5 minutes * 60 * 20

    private static final Pattern TIP_RESPONSE_PATTERN = Pattern.compile(
            "^You tipped \\d+ players? in \\d+ (?:different )?games?!$");

    private static final Runnable tipTask = () -> {
        var config = ModConfigManager.get().autotip;
        if (config.enabled && HypixelLocationTracker.getInstance().isOnHypixel()) {
            ChatUtils.sendCommand("tip all");
        }
    };

    private AutotipModule() {}

    public static void init() {
        Scheduler.scheduleRepeating(TIP_INTERVAL_TICKS, tipTask);

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            if (!ModConfigManager.get().autotip.hideMessages) return true;
            if (!HypixelLocationTracker.getInstance().isOnHypixel()) return true;

            String text = ChatUtils.stripColor(message.getString());

            // 匹配 "You tipped X player(s) in X (different) game(s)!"
            if (TIP_RESPONSE_PATTERN.matcher(text).matches()) {
                return false;
            }

            // 匹配 "You already tipped everyone..."
            if (text.equals("You already tipped everyone that has boosters active, so there isn't anybody to be tipped right now!")) {
                return false;
            }

            return true;
        });
    }
}
