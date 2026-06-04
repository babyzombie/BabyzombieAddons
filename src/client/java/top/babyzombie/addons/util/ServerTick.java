package top.babyzombie.addons.util;

/**
 * Simple clock based on {@link System#currentTimeMillis()}.
 */
public final class ServerTick {
    private ServerTick() {}

    /** @return current system time in milliseconds. */
    public static long getTime() {
        return System.currentTimeMillis();
    }

    /** @return approximate tick count based on system time (50ms per tick). */
    public static int getTick() {
        return (int) (System.currentTimeMillis() / 50);
    }
}
