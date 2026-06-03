package top.babyzombie.addons.util;

/**
 * Simple clock based on {@link System#currentTimeMillis()}.
 */
public final class ServerTick {
    private ServerTick() {}

    public static long getTime() {
        return System.currentTimeMillis();
    }

    public static int getTick() {
        return (int) (System.currentTimeMillis() / 50);
    }
}
