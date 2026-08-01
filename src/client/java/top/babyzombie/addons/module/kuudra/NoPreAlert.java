package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
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
    private static final long EARLY_DELAY_MS = 10_000; // 与 IQ 一致：P1 开始后 10 秒才考虑提示
    private static final long FALLBACK_CONFIRM_DELAY_MS = 350; // 与 IQ 一致：延迟到达后再确认 350ms
    private static final int MIN_EMPTY_SCANS_FOR_FALLBACK = 3; // 与 IQ 一致：连续 3 次空扫描

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
    /** 当前缺失 pre 的名称（队伍消息/自己检测动态更新），用于放置点红色标记 */
    private static String missingPreName;

    /** Current pre spot name (Triangle/X/Equals/Slash) or null if not detected. */
    public static String getPreSpotName() {
        return detectedPre != null ? detectedPre.name : null;
    }

    /** 当前缺失 pre 名称（动态：别人发 "No X!" 或自己检测到时更新），null 表示未知。 */
    public static String getMissingPreName() {
        return missingPreName;
    }
    private static long supplyStartMs = -1; // <0 表示 P1 还没开始，不检测
    private static int emptyScans;
    private static boolean fallbackPending;
    private static long fallbackStartMs;
    private static boolean carrierCheckAttempted; // 检测到箱子后的 No Pre 检查只做一次

    public static void init() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            detectedPre = null;
            checked = false;
            missingPreName = null;
            supplyStartMs = -1;
            emptyScans = 0;
            fallbackPending = false;
            carrierCheckAttempted = false;
        });

        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            if (!ModConfigManager.get().kuudra.phase1.noPreAlert) return;
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return;
            String text = ChatUtils.stripColor(msg.getString());

            if (text.contains("Okay adventurers, I will go and fish up Kuudra")) {
                detectedPre = null;
                checked = false;
                missingPreName = null;
                supplyStartMs = System.currentTimeMillis();
                emptyScans = 0;
                carrierCheckAttempted = false;
                return;
            }
            if (text.contains("OMG! Great work collecting my supplies")) {
                checked = true; // supplies phase over, stop checking
                return;
            }
            // Detect "No X!" from party chat (IQ or our own) → 更新缺失 pre（动态标记）
            Matcher nm = NO_PRE_INCOMING.matcher(text);
            if (nm.find()) {
                missingPreName = normalizePreName(nm.group(1));
                checked = true; // someone already reported, don't duplicate
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
            if (supplyStartMs < 0) return; // P1 还没开始

            long elapsed = System.currentTimeMillis() - supplyStartMs;
            long adaptiveDelay = EARLY_DELAY_MS; // Could add TPS/ping adjustment here

            // Scan for supply giants
            var giants = client.player.level().getEntitiesOfClass(Giant.class,
                    new AABB(client.player.blockPosition()).inflate(64),
                    g -> g.getY() < 67);

            if (!giants.isEmpty()) {
                emptyScans = 0;
                fallbackPending = false;
                // Try to detect pre spot once we see supplies
                if (detectedPre == null) detectPreSpot();
                // 检测到箱子后只检查一次（对齐 IQ 的 carrier check），避免反复误报
                if (elapsed >= adaptiveDelay && detectedPre != null && !carrierCheckAttempted) {
                    carrierCheckAttempted = true;
                    checkAndAlert();
                }
            } else {
                emptyScans++;
                // IQ 机制：延迟到达后标记 pending，再确认 350ms 且连续 3 次空扫描才发，避免误报
                if (elapsed >= adaptiveDelay) {
                    if (!fallbackPending) {
                        fallbackPending = true;
                        fallbackStartMs = System.currentTimeMillis();
                    } else {
                        long confirmElapsed = System.currentTimeMillis() - fallbackStartMs;
                        if (confirmElapsed >= FALLBACK_CONFIRM_DELAY_MS
                                && emptyScans >= MIN_EMPTY_SCANS_FOR_FALLBACK) {
                            if (detectedPre == null) detectPreSpot();
                            checkAndAlert();
                        }
                    }
                }
            }
        });
    }

    /** 把 "No X!" 消息里的别名（tri/eq/xc 等）归一化为标准 pile 名。 */
    private static String normalizePreName(String alias) {
        return switch (alias.toLowerCase(Locale.ROOT)) {
            case "tri", "triangle" -> "Triangle";
            case "eq", "equals" -> "Equals";
            case "xc", "x cannon" -> "X Cannon";
            case "x" -> "X";
            case "slash" -> "Slash";
            case "shop" -> "Shop";
            default -> alias;
        };
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

        // 对齐 IQ：用 crate 位置判断 pre spot 18 格内有没有补给
        // （之前用巨人实体 AABB 检测，y 范围写错导致永远误报）
        List<Giant> carriers = player.level().getEntitiesOfClass(Giant.class,
                new AABB(player.blockPosition()).inflate(64), g -> g.getY() < 67);
        boolean hasPre = carriers.stream().anyMatch(g -> {
            double angleRad = Math.toRadians(g.getYRot() + 130.0f);
            double cx = g.getX() + (3.7 * Math.cos(angleRad));
            double cz = g.getZ() + (3.7 * Math.sin(angleRad));
            return Math.hypot(cx - detectedPre.pos.x, cz - detectedPre.pos.z) < 18;
        });

        if (!hasPre) {
            missingPreName = detectedPre.name;
            ChatUtils.sendCommand("pc No " + detectedPre.name + "!");
            sendSecondSupplyHint();
        }
    }

    /** 自己点位没箱子时，提示最近的另一个补给点位（类似 IQ 的 Second Supply Alert）。 */
    private static void sendSecondSupplyHint() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        List<Giant> giants = player.level().getEntitiesOfClass(Giant.class,
                new AABB(player.blockPosition()).inflate(64), g -> g.getY() < 67);
        if (giants.isEmpty()) return;

        // 最近的补给巨人 → crate 位置 → 匹配最近 pile 名
        Giant nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (var g : giants) {
            double d = g.distanceToSqr(player);
            if (d < bestDist) {
                bestDist = d;
                nearest = g;
            }
        }
        if (nearest == null) return;

        double angleRad = Math.toRadians(nearest.getYRot() + 130.0f);
        double cx = nearest.getX() + (3.7 * Math.cos(angleRad));
        double cz = nearest.getZ() + (3.7 * Math.sin(angleRad));

        String name = KuudraPileWaypoints.findNearestPileName(cx, cz);
        if (name != null) {
            // 声音 + title 提示去最近的补给点位（不发 party 消息）
            var p = Minecraft.getInstance().player;
            if (p != null) {
                p.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), 1.3f, 1.6f);
            }
            ChatUtils.showTitle(String.format("§aGo %s! §7(x: %.0f, z: %.0f)", name, cx, cz));
        }
    }
}
