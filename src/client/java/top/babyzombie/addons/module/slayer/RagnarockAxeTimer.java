package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Tracks Ragnarock Axe cast time, duration, and cooldown.
 */
public final class RagnarockAxeTimer {
    static long castTime;
    static long duration;
    static long cooldown;
    static boolean cancelled;
    static boolean finished;

    private RagnarockAxeTimer() {}

    public static void init() {
        if (ModConfigManager.get().slayer.ragnarockAxeTimer) {
            ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
                if (overlay) return;
                String text = message.getString();
                if (text.contains("CASTING IN 3s")) {
                    castTime = System.currentTimeMillis() + 2800;
                    cooldown = System.currentTimeMillis() + 19800;
                    cancelled = false;
                    finished = false;
                }
                if (text.contains("Ragnarock was cancelled")) {
                    cancelled = true;
                    castTime = 0;
                    duration = 0;
                    cooldown = System.currentTimeMillis() + 19800;
                }
            });
        }
    }

    static void update() {
        long now = System.currentTimeMillis();
        if (castTime > 0 && castTime <= now && duration == 0) {
            duration = now + 10000;
            castTime = 0;
        }
        if (duration > 0 && duration <= now) {
            duration = 0;
            finished = true;
        }
        if (cooldown > 0 && cooldown <= now && castTime == 0 && duration == 0) {
            cooldown = 0;
        }
    }
}
