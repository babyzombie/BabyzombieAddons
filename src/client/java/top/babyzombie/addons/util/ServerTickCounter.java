package top.babyzombie.addons.util;

import com.google.common.collect.EvictingQueue;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ClientboundPingPacket;

import java.util.Queue;

/**
 * Estimates server TPS from incoming packets.
 * <p>
 * Two strategies:
 * <ol>
 *   <li><b>Ping-packet counting</b> (Hypixel) — {@link ClientboundPingPacket#getId()}
 *       increments each server tick, giving exact tick count.</li>
 *   <li><b>Packet-rate estimation</b> (generic) — counts how many client ticks per second
 *       received a packet, averaged over 12 seconds.</li>
 * </ol>
 */
public final class ServerTickCounter {

    private static final int TARGET_TPS = 20;
    private static final int WINDOW_SECONDS = 12;
    private static final long WORLD_CHANGE_GRACE = 5000L;

    // --- ping-based tick counting (cross-thread: written by net thread, read by render thread) ---
    private static volatile int totalTicks;
    private static int lastPingId;
    private static volatile boolean hasPingData;
    private static volatile long wallTimeAtFirstPing;

    // --- real ping via PingDebugMonitor (cross-thread) ---
    private static volatile long lastPingResult = -1;

    // --- packet-rate TPS estimation (render thread only) ---
    private static final Queue<Integer> tpsSamples = EvictingQueue.create(WINDOW_SECONDS);
    private static boolean receivedPacketThisTick;
    private static int ticksWithPacketsThisSecond;
    private static volatile double estimatedTps;
    private static long lastWorldChange;

    private static boolean initialized;

    private ServerTickCounter() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientTickEvents.END_CLIENT_TICK.register(ServerTickCounter::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (client.player.tickCount % 20 == 0) {
                calculateTickRate();
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    // --- Called from mixins ---

    /** Called from handlePing mixin (network thread). */
    public static void onServerTick(ClientboundPingPacket packet) {
        if (packet.getId() != lastPingId) {
            if (!hasPingData) {
                hasPingData = true;
                wallTimeAtFirstPing = System.currentTimeMillis();
                totalTicks = 0;
            } else {
                int diff = packet.getId() - lastPingId;
                if (diff > 0 && diff < 1000) {
                    totalTicks += diff;
                } else {
                    // Overflow or abnormal jump — recalibrate
                    wallTimeAtFirstPing = System.currentTimeMillis();
                    totalTicks = 0;
                }
            }
            lastPingId = packet.getId();
        }
    }

    /** Called from Connection mixin (network thread). */
    public static void onReceivePacket() {
        Minecraft.getInstance().execute(() -> receivedPacketThisTick = true);
    }

    /** Called from PingDebugMonitor mixin (network thread). */
    public static void onPingResult(long ping) {
        lastPingResult = ping;
    }

    // --- internal ---

    private static void onClientTick(Minecraft client) {
        if (receivedPacketThisTick) {
            ticksWithPacketsThisSecond++;
            receivedPacketThisTick = false;
        }
    }

    private static void calculateTickRate() {
        if (ticksWithPacketsThisSecond > 0) {
            tpsSamples.offer(ticksWithPacketsThisSecond);
        }

        if (lastWorldChange + WORLD_CHANGE_GRACE < System.currentTimeMillis()) {
            double avg = tpsSamples.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0d);
            estimatedTps = Math.clamp(Math.round(avg * 10.0) / 10.0, 0.0, TARGET_TPS);
            ticksWithPacketsThisSecond = 0;
        }
    }

    private static void reset() {
        tpsSamples.clear();
        totalTicks = 0;
        lastPingId = 0;
        hasPingData = false;
        wallTimeAtFirstPing = 0;
        receivedPacketThisTick = false;
        ticksWithPacketsThisSecond = 0;
        estimatedTps = 0.0;
        lastPingResult = -1;
        lastWorldChange = System.currentTimeMillis();
    }

    // --- public API ---

    /**
     * @return time in milliseconds driven by server ticks. Each server tick
     *         advances the clock by 50ms. Between ticks the value stays the same.
     *         Falls back to {@link System#currentTimeMillis()} if no ping data.
     */
    public static long getAdjustedTime() {
        if (!hasPingData) return System.currentTimeMillis();
        return wallTimeAtFirstPing + (long) totalTicks * 50;
    }

    /** @return current estimated TPS (0–20), or 20 if no data yet. */
    public static double getTickRate() {
        if (!hasPingData) return TARGET_TPS;
        if (estimatedTps <= 0.0) return TARGET_TPS;
        return estimatedTps;
    }

    /** @return total server ticks counted via ping packets. */
    public static int getTotalTicks() {
        return totalTicks;
    }

    /** @return true if we have received at least one ping packet in the current world. */
    public static boolean hasPingData() {
        return hasPingData;
    }

    /** @return the real network round-trip time in ms, or -1 if no measurement yet. */
    public static long getPing() {
        return lastPingResult;
    }
}
