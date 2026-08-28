package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.EnumMap;
import java.util.Map;

/**
 * Kuudra 各阶段计时器，按 tier 跟踪不同阶段并以 HUD 形式显示。
 *
 * <p>T5: Supplies → Build → Eaten → Stun → DPS → Skip → Boss<br>
 * T3/T4: Supplies → Build → Eaten → Stun → DPS<br>
 * T1/T2: Supplies → Build → Fuel</p>
 */
public final class KuudraPhaseTimer {
    private KuudraPhaseTimer() {}

    // ── Phase enum ──
    public enum Phase {
        SUPPLIES("Supplies", 22.0, 23.0, 24.6, 27.0, 30.0, 34.0),
        BUILD("Build", 12.0, 13.5, 15.0, 17.0, 19.0, 20.0),
        EATEN("Eaten", 4.0, 4.4, 5.0, 5.5, 6.0, 7.0),
        STUN("Stun", 0.0, 0.0, 0.0, 0.1, 0.3, 0.8),
        DPS("DPS", 2.5, 3.2, 3.6, 3.8, 4.2, 4.5),
        SKIP("Skip", 3.5, 4.0, 4.6, 4.8, 5.0, 5.2),
        BOSS("Boss", 1.7, 2.0, 2.3, 2.8, 3.3, 4.0),
        FUEL("Fuel", 25.0, 30.0, 35.0, 40.0, 50.0, 60.0);

        final String label;
        final double[] thresholds;

        Phase(String label, double... thresholds) {
            this.label = label;
            this.thresholds = thresholds;
        }
    }

    // ── Tier-specific phase sets ──
    private static final Phase[] T5_PHASES   = {Phase.SUPPLIES, Phase.BUILD, Phase.EATEN, Phase.STUN, Phase.DPS, Phase.SKIP, Phase.BOSS};
    private static final Phase[] T34_PHASES  = {Phase.SUPPLIES, Phase.BUILD, Phase.EATEN, Phase.STUN, Phase.DPS};
    private static final Phase[] T12_PHASES  = {Phase.SUPPLIES, Phase.BUILD, Phase.FUEL};

    // ── State ──
    private static final Map<Phase, Double> splits = new EnumMap<>(Phase.class);
    private static Phase currentPhase;
    private static long phaseStartTick;
    private static int kuudraTier;

    // ── Public accessors ──
    public static Map<Phase, Double> getSplits() { return new EnumMap<>(splits); }
    public static Phase currentPhase() { return currentPhase; }
    public static boolean isInRun() { return kuudraTier > 0 && currentPhase != null; }

    /** 当前阶段开始时刻（ServerTick.getTime() 时钟源），供其他功能对齐阶段计时 */
    public static long getPhaseStartTick() { return phaseStartTick; }

    public static void reset() {
        kuudraTier = 0;
        currentPhase = null;
        phaseStartTick = 0;
        for (Phase p : Phase.values()) splits.put(p, 0.0);
    }

    public static void init() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> reset());
        for (Phase p : Phase.values()) splits.put(p, 0.0);

        // ── Chat-based phase detection ──
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!ModConfigManager.get().kuudra.phaseTimer) return true;
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return true;
            String text = ChatUtils.stripColor(message.getString());

            if (KuudraChatLines.isFishUpKuudra(text)) {
                startRun();
                return true;
            }
            if (currentPhase == null) return true;

            // KUUDRA DOWN — ends any tier
            if (KuudraChatLines.isKuudraDown(text)) {
                endRun();
                return true;
            }

            if (!text.startsWith("[NPC] Elle:") && !KuudraChatLines.isDestroyedPod(text)
                    && !KuudraChatLines.isEatenByKuudra(text))
                return true;

            if (KuudraChatLines.isSuppliesCollected(text)) {
                endPhase(Phase.SUPPLIES);
            } else if (KuudraChatLines.isBallistaReady(text)) {
                if (isT12()) {
                    // T1/T2: BUILD → FUEL
                    endPhase(Phase.BUILD);
                } else {
                    // T3+: BUILD → EATEN
                    endPhase(Phase.BUILD);
                }
            } else if (KuudraChatLines.isEatenByKuudra(text) && !text.contains("Elle")) {
                if (currentPhase == Phase.EATEN && isT3Plus()) endPhase(Phase.EATEN);
            } else if (KuudraChatLines.isDestroyedPod(text)) {
                if (currentPhase == Phase.STUN && isT3Plus()) endPhase(Phase.STUN);
            } else if (KuudraChatLines.isP4Start(text)) {
                if (isT5()) {
                    endPhase(Phase.DPS);
                } else if (isT3Plus()) {
                    // T3/T4: DPS done → end run (no SKIP/BOSS)
                    endPhase(Phase.DPS);
                    endRun();
                }
            } else if (KuudraChatLines.isGoodJob(text)) {
                endRun();
            }
            return true;
        });

        // ── Tick-based BOSS detection (Y < 10, T5 only) ──
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (currentPhase == null || currentPhase == Phase.BOSS) return;
            if (isT5() && client.player != null && client.player.getY() < 10
                    && (currentPhase == Phase.SKIP || currentPhase == Phase.DPS)) {
                endPhase(currentPhase);
            }
        });

        // ── HUD rendering ──
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_splits"),
                (context, tickCounter) -> {
                    if (!ModConfigManager.get().kuudra.phaseTimer) return;
                    boolean hasData = currentPhase != null
                            || splits.values().stream().anyMatch(v -> v > 0.0);
                    if (!hasData) return;

                    long now = ServerTick.getTime();
                    var font = Minecraft.getInstance().font;
                    int x = HudManager.x("KuudraSplits"), y = HudManager.y("KuudraSplits");
                    float s = HudManager.scale("KuudraSplits");

                    StringBuilder sb = new StringBuilder();
                    sb.append("§b§lKuudra Splits");
                    double overall = 0;

                    for (Phase p : activePhases()) {
                        double time = splits.getOrDefault(p, 0.0);
                        if (currentPhase != null && p == currentPhase && phaseStartTick > 0) {
                            time = (now - phaseStartTick) / 1000.0;
                        }
                        overall += time;
                        sb.append('\n').append(formatLine(p.label, time, p.thresholds));
                    }

                    sb.append('\n').append(formatOverall("Overall", overall));
                    HudManager.drawScaled(context, font, sb.toString(), x, y, s);
                });
    }

    // ── Phase advancement ──

    private static void startRun() {
        kuudraTier = detectTier();
        for (Phase p : Phase.values()) splits.put(p, 0.0);
        currentPhase = Phase.SUPPLIES;
        phaseStartTick = ServerTick.getTime();
    }

    private static void endPhase(Phase completedPhase) {
        if (completedPhase != currentPhase) return;
        long now = ServerTick.getTime();
        splits.put(completedPhase, (now - phaseStartTick) / 1000.0);

        Phase[] phases = activePhases();
        int idx = -1;
        for (int i = 0; i < phases.length; i++) {
            if (phases[i] == completedPhase) { idx = i; break; }
        }
        if (idx >= 0 && idx + 1 < phases.length) {
            currentPhase = phases[idx + 1];
            phaseStartTick = now;
            // T5 Skip → Boss:进入最终阶段后 stun HUD 的 "P4" 倒计时已无意义,清除
            if (currentPhase == Phase.BOSS) KuudraStunTimer.clearP4Timer();
        } else {
            currentPhase = null;
        }
    }

    private static void endRun() {
        if (currentPhase != null && phaseStartTick > 0) {
            long now = ServerTick.getTime();
            splits.put(currentPhase, (now - phaseStartTick) / 1000.0);
        }
        currentPhase = null;
    }

    // ── Tier detection ──

    private static int detectTier() {
        var loc = HypixelLocationTracker.getInstance().getLocation();
        if (loc == null) return 0;
        for (int t = 5; t >= 1; t--) {
            if (loc.contains("T" + t)) return t;
        }
        return 0;
    }

    private static boolean isT5()    { return kuudraTier >= 5; }
    private static boolean isT3Plus() { return kuudraTier >= 3; }
    private static boolean isT12()    { return kuudraTier >= 1 && kuudraTier <= 2; }

    private static Phase[] activePhases() {
        if (isT5()) return T5_PHASES;
        if (isT3Plus()) return T34_PHASES;
        return T12_PHASES;
    }

    // ── Display formatting ──

    private static String formatLine(String label, double seconds, double[] thresholds) {
        return String.format("§3%s %s%.2fs", label, splitColor(seconds, thresholds), seconds);
    }

    private static String formatOverall(String label, double seconds) {
        return String.format("§3%s %s%.2fs", label, overallColor(seconds), seconds);
    }

    private static String splitColor(double time, double[] thresholds) {
        if (time <= 0) return "§f";
        if (time <= thresholds[0]) return "§f";
        if (time <= thresholds[1]) return "§5";
        if (time <= thresholds[2]) return "§9";
        if (time <= thresholds[3]) return "§a";
        if (time <= thresholds[4]) return "§6";
        if (time <= thresholds[5]) return "§c";
        return "§4";
    }

    private static String overallColor(double time) {
        if (time <= 0) return "§f";
        if (time >= 52.5 && time <= 59.0) return "§9";
        if (time >= 50.0 && time <= 52.49) return "§5";
        return splitColor(time, new double[]{50.0, 52.49, 59.49, 65.0, 70.0, 80.0});
    }
}
