package top.babyzombie.addons.util;

/**
 * Server-tick-synchronized clock. Time advances by 50ms for every
 * ClientboundSetTimePacket received (server sends one per tick via
 * ServerTickMixin hooking ClientPacketListener.handleSetTime).
 */
public final class ServerTick {
    static volatile long time = System.currentTimeMillis();
    static volatile int tick;

    private ServerTick() {}

    public static long getTime() {
        return time;
    }

    public static int getTick() {
        return tick;
    }

    public static void onPacket() {
        time += 50;
        tick++;
    }
}
