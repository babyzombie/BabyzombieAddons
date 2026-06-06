package top.babyzombie.addons.util;

import top.babyzombie.addons.config.ModConfigManager;

/**
 * Time utility that can use either wall-clock time or server-TPS-adjusted time.
 * <p>
 * When {@code useTpsAdjustedTime} is enabled in config, {@link #getTime()} returns
 * time computed from actual server ticks (via {@link ServerTickCounter}), so cooldown
 * timers automatically slow down on laggy servers.
 */
public final class ServerTick {
    private ServerTick() {}

    /**
     * @return current time in milliseconds. Uses TPS-adjusted time when
     *         the config toggle is enabled and ping data is available;
     *         falls back to {@link System#currentTimeMillis()} otherwise.
     */
    public static long getTime() {
        if (ModConfigManager.get().general.useTpsAdjustedTime && ServerTickCounter.hasPingData()) {
            return ServerTickCounter.getAdjustedTime();
        }
        return System.currentTimeMillis();
    }

    /**
     * @return tick count. When TPS-adjusted, returns actual server ticks;
     *         otherwise approximate based on system time (50ms per tick).
     */
    public static int getTick() {
        if (ModConfigManager.get().general.useTpsAdjustedTime && ServerTickCounter.hasPingData()) {
            return ServerTickCounter.getTotalTicks();
        }
        return (int) (System.currentTimeMillis() / 50);
    }

    /**
     * @return the current estimated server TPS (0–20), or 20 if no data.
     */
    public static double getTps() {
        return ServerTickCounter.getTickRate();
    }

    /**
     * @return the player's real network latency (keep-alive RTT) in milliseconds,
     *         or -1 if no measurement yet.
     */
    public static int getPing() {
        long ping = ServerTickCounter.getPing();
        return ping >= 0 ? (int) ping : -1;
    }
}
