package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ServerTick;

/**
 * Tracks Ragnarock Axe cast time, duration, and cooldown with phase awareness.
 */
public final class RagnarockAxeTimer {
    static long castTime;
    static long duration;
    static long cooldown;
    static boolean cancelled;
    static boolean finished;
    static String text = "";

    private RagnarockAxeTimer() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            var cfg = ModConfigManager.get().slayer;
            if (cfg.ragnarockAxeTimer == ModConfig.RagnarockAxeMode.OFF) return;

            String text = message.getString();

            if (overlay) {
                // Action bar
                if (text.contains("CASTING IN 3s") && cooldown < ServerTick.getTime()) {
                    castTime = ServerTick.getTime() + 3000 - ServerTick.getPing();
                    cooldown = ServerTick.getTime() + 20000 - ServerTick.getPing();
                    cancelled = false;
                    finished = false;
                }
                if (text.contains("CANCELLED")) {
                    cancelled = true;
                    castTime = 0;
                    duration = 0;
                    cooldown = ServerTick.getTime() + 20000 - ServerTick.getPing();
                    finished = false;
                }
            } else {
                // Chat (exact match, won't catch player chat)
                if (ChatUtils.stripColor(text).trim().equals("Ragnarock was cancelled due to taking damage!")) {
                    cancelled = true;
                    castTime = 0;
                    duration = 0;
                    cooldown = ServerTick.getTime() + 20000 - ServerTick.getPing();
                    finished = false;
                }
            }
        });
    }

    static void update() {
        long now = ServerTick.getTime();
        if (castTime == 0 && duration == 0 && cooldown == 0) { text = ""; return; }

        int mode = ModConfigManager.get().slayer.ragnarockAxeTimer.ordinal();

        if (castTime > 0) {
            if (castTime > now) {
                makeCastText(castTime - now, mode);
            } else {
                duration = now + 10000;
                castTime = 0;
            }
        } else if (duration > 0) {
            if (duration > now) {
                makeActiveText(duration - now, mode);
            } else {
                duration = 0;
                finished = true;
            }
        } else if (cooldown > now && castTime == 0 && duration == 0) {
            makeCooldownText(cooldown - now, mode);
        } else if (cooldown > 0 && cooldown <= now && castTime == 0 && duration == 0) {
            cooldown = 0;
        }
        if (castTime == 0 && duration == 0 && cooldown == 0) text = "";
    }

    private static void makeCastText(long rem, int mode) {
        switch (mode) {
            case 1 -> text = "§5Ragnarock: §b" + ChatUtils.formatTime(rem);
            case 2 -> {
                int n = Math.min(20, (int)((3000 - rem) / (3000.0 / 20)) + 1);
                text = "§5Ragnarock: §b[§a" + "|".repeat(n) + "§e" + "|".repeat(20 - n) + "§b]";
            }
            default -> text = "";
        }
    }

    private static void makeActiveText(long rem, int mode) {
        switch (mode) {
            case 1 -> text = "§5Ragnarock: §a" + ChatUtils.formatTime(rem);
            case 2 -> {
                int n = Math.min(20, (int)(rem / (10000.0 / 20)) + 1);
                text = "§5Ragnarock: §b[§a" + "|".repeat(n) + "§c" + "|".repeat(20 - n) + "§b]";
            }
            default -> text = "";
        }
    }

    private static void makeCooldownText(long rem, int mode) {
        switch (mode) {
            case 1 -> text = "§5Ragnarock: §c" + ChatUtils.formatTime(rem);
            case 2 -> {
                int total = finished ? 7000 : 20000;
                long elapsed = total - rem;
                int n = Math.min(20, (int)(elapsed / (total / 20.0)));
                text = "§5Ragnarock: §b[§e" + "|".repeat(n) + "§c" + "|".repeat(20 - n) + "§b]";
            }
            default -> text = "";
        }
    }
}
