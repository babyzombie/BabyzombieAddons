package top.babyzombie.addons.module.slayer;

import top.babyzombie.addons.util.ServerTick;

/**
 * Holy Ice timer for Rift area.
 */
public final class HolyIceTimer {
    static long time;
    static boolean activated;
    static String text = "";

    private HolyIceTimer() {}

    static void updateText() {
        if (time == 0) return;
        long now = ServerTick.getTime();
        if (activated) {
            long rem = 1500 - (now - time);
            if (rem <= 0) { activated = false; time = now; }
            else text = "§bHoly Ice: §a" + BossDetector.formatTime(rem);
        } else {
            long rem = 2500 - (now - time);
            if (rem <= 0) { time = 0; text = ""; }
            else text = "§bHoly Ice: §e" + BossDetector.formatTime(rem);
        }
    }
}
