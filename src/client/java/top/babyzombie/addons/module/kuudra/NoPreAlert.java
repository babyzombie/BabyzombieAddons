package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * No Pre 提醒 — 持续检测当前 pre spot 是否有补给箱，无则通知队伍。
 */
public final class NoPreAlert {
    private NoPreAlert() {}

    private static final int SCAN_INTERVAL_TICKS = 4;
    private static final long EARLY_DELAY_MS = 4_000;

    // Detect "No Triangle!", "No X!", etc. in party chat (from IQ or our own)
    private static final Pattern NO_PRE_INCOMING = Pattern.compile(
            "Party > .+?: (?:\\[IQ] )?(?:no|missing) (triangle|tri|x|xc|x ?cannon|equals|eq|slash|shop|square)\\b",
            Pattern.CASE_INSENSITIVE);

    // 4 Kuudra pre spots with coordinates, detection radius, and pile name
    private enum PreSpot {
        TRIANGLE("Triangle", -67.5, 77, -122.5, 15.0),
        X("X", -142.5, 77, -151, 15.0),
        EQUALS("Equals", -65.5, 76, -87.5, 15.0),
        SLASH("Slash", -113.5, 77, -68.5, 15.0);

        final String name;
        final Vec3 pos;
        final double radius;

        PreSpot(String name, double x, double y, double z, double radius) {
            this.name = name;
            this.pos = new Vec3(x, y, z);
            this.radius = radius;
        }

        static PreSpot detect(Vec3 playerPos) {
            for (var s : values()) {
                if (playerPos.distanceToSqr(s.pos) < s.radius * s.radius) return s;
            }
            return null;
        }
    }

    private static PreSpot detectedPre;
    private static boolean checked;

    /** Current pre spot name (Triangle/X/Equals/Slash) or null if not detected. */
    public static String getPreSpotName() {
        return detectedPre != null ? detectedPre.name : null;
    }
    private static long supplyStartMs;
    private static int emptyScans;

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            if (!ModConfigManager.get().kuudra.phase1.noPreAlert) return;
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return;
            String text = ChatUtils.stripColor(msg.getString());

            if (text.contains("Okay adventurers, I will go and fish up Kuudra")) {
                detectedPre = null;
                checked = false;
                supplyStartMs = System.currentTimeMillis();
                emptyScans = 0;
                return;
            }
            if (text.contains("OMG! Great work collecting my supplies")) {
                checked = true; // supplies phase over, stop checking
                return;
            }
            // Detect "No X!" from party chat (IQ or our own)
            if (!checked) {
                Matcher nm = NO_PRE_INCOMING.matcher(text);
                if (nm.find()) {
                    checked = true; // someone already reported, don't duplicate
                }
            }

            // Elle flies over → detect pre spot from player position
            if (text.contains("Head over to the main platform")) {
                detectPreSpot();
            }
            // Elle reports no crate → re-scan
            if (text.contains("Not again!") && !checked) {
                checkAndAlert();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().kuudra.phase1.noPreAlert) return;
            if (checked) return;
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            if (client.player == null || client.player.tickCount % SCAN_INTERVAL_TICKS != 0) return;

            long elapsed = System.currentTimeMillis() - supplyStartMs;
            long adaptiveDelay = EARLY_DELAY_MS; // Could add TPS/ping adjustment here

            // Scan for supply giants
            var giants = client.player.level().getEntitiesOfClass(Giant.class,
                    new AABB(client.player.blockPosition()).inflate(64),
                    g -> g.getY() < 67);

            if (!giants.isEmpty()) {
                emptyScans = 0;
                // Try to detect pre spot once we see supplies
                if (detectedPre == null) detectPreSpot();
                // Still check after delay even if supplies are seen
                if (elapsed >= adaptiveDelay && detectedPre != null) {
                    checkAndAlert();
                }
            } else {
                emptyScans++;
                if (elapsed >= adaptiveDelay && emptyScans >= 3) {
                    if (detectedPre == null) detectPreSpot();
                    checkAndAlert();
                }
            }
        });
    }

    private static void detectPreSpot() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        detectedPre = PreSpot.detect(player.position());
    }

    private static void checkAndAlert() {
        if (detectedPre == null) return;
        checked = true;

        var player = Minecraft.getInstance().player;
        if (player == null) return;

        // Check if there's a supply giant near the pre spot
        List<Giant> nearby = player.level().getEntitiesOfClass(Giant.class,
                new AABB(detectedPre.pos.add(-18, -5, -18), detectedPre.pos.add(18, 10, 18)),
                g -> g.getY() < 67);

        if (nearby.isEmpty()) {
            ChatUtils.sendCommand("pc No " + detectedPre.name + "!");
        }
    }
}
