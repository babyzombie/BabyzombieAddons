package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.render.WorldRenderUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * P4 功能集合 — Rend 伤害追踪、Ichor Pool 区域、Kuudra 距离显示。
 */
public final class KuudraP4Features {
    private KuudraP4Features() {}

    // ── Rend ──
    private static final float MIN_REND_RAW = 1666f;
    private static final float REND_MULTIPLIER = 9600f;
    private static float lastHP = -1;
    private static long bossStartMs;

    // ── Ichor Pool ──
    // Party messages: "Ichor Pool at (x, y, z)" or "[IQ] Ichor Pool Casted at x, y, z!"
    private static final Pattern ICHOR_PARTY = Pattern.compile(
            "(?i).*(?:ichor\\s+pool).*?\\(?\\s*(-?\\d+)\\s*[, ]+\\s*(-?\\d+)\\s*[, ]+\\s*(-?\\d+)");
    // Self-cast: Hypixel "Casting Spell: Ichor Pool!"
    private static final Pattern ICHOR_SELF = Pattern.compile(
            "(?i).*casting\\s+(?:spell:\\s*)?ichor\\s+pool");
    private static final float ICHOR_RADIUS = 8f;
    private static final long ICHOR_DURATION_MS = 20_000;
    private record IchorPool(Vec3 center, long expiresAt) {}
    private static final List<IchorPool> ichorPools = new ArrayList<>();

    // ── Distance ──
    private static float kuudraDist = -1;

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return;

            String text = ChatUtils.stripColor(msg.getString());
            // Reset rend tracking on boss start
            if (text.contains("Okay adventurers, I will go and fish up Kuudra")) {
                lastHP = -1;
                ichorPools.clear();
                return;
            }
            // Start boss timer
            if (text.contains("POW! SURELY THAT'S IT")) {
                bossStartMs = System.currentTimeMillis();
            }

            // Ichor Pool
            var cfg4 = ModConfigManager.get().kuudra.phase4;
            if (cfg4.ichorPoolWaypoints) {
                // Self-cast: forward player position to party
                if (ICHOR_SELF.matcher(text).matches()) {
                    var p = Minecraft.getInstance().player;
                    if (p != null) {
                        var bp = p.blockPosition();
                        ChatUtils.sendCommand("pc Ichor Pool at (" +
                                bp.getX() + ", " + bp.getY() + ", " + bp.getZ() + ")");
                    }
                }

                // Parse party messages (ours and IQ's) for Ichor Pool positions
                Matcher im = ICHOR_PARTY.matcher(text);
                if (im.find()) {
                    try {
                        int cx = Integer.parseInt(im.group(1));
                        int cy = Integer.parseInt(im.group(2));
                        int cz = Integer.parseInt(im.group(3));
                        ichorPools.add(new IchorPool(new Vec3(cx + 0.5, cy + 0.05, cz + 0.5),
                                System.currentTimeMillis() + ICHOR_DURATION_MS));
                    } catch (NumberFormatException ignored) {}
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            var cfg = ModConfigManager.get().kuudra.phase4;
            if (client.player == null) return;

            boolean inP4 = KuudraLocationTracker.p4 || "p4".equals(KuudraLocationTracker.area);
            if (!inP4) return;

            LivingEntity kuudra = KuudraLocationTracker.kuudraEntity;
            if (kuudra == null || kuudra.isDeadOrDying()) {
                // Fallback: find wither named Kuudra
                var withers = client.player.level().getEntitiesOfClass(
                        net.minecraft.world.entity.boss.wither.WitherBoss.class,
                        new net.minecraft.world.phys.AABB(client.player.blockPosition()).inflate(128),
                        w -> ChatUtils.stripColor(w.getName().getString()).contains("Kuudra"));
                if (!withers.isEmpty()) kuudra = withers.getFirst();
            }

            if (kuudra != null && !kuudra.isDeadOrDying()) {
                // Rend tracking
                if (cfg.rendTracker) {
                    float hp = kuudra.getHealth();
                    if (lastHP > 0 && hp > 0 && lastHP - hp > MIN_REND_RAW) {
                        float scaled = (lastHP - hp) * REND_MULTIPLIER;
                        double sec = (System.currentTimeMillis() - bossStartMs) / 1000.0;
                        if (sec > 1.2) {
                            ChatUtils.showTranslatable("kuudra.rend", formatRendDamage(scaled), sec);
                        }
                    }
                    lastHP = hp;
                }

                // Distance
                if (cfg.kuudraDistance) {
                    kuudraDist = (float) kuudra.distanceTo(client.player);
                }
            }

            // Expire ichor pools
            long now = System.currentTimeMillis();
            Iterator<IchorPool> it = ichorPools.iterator();
            while (it.hasNext()) {
                if (it.next().expiresAt <= now) it.remove();
            }
        });

        // Kuudra distance HUD
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_distance"),
                (context, tickCounter) -> {
                    if (!ModConfigManager.get().kuudra.phase4.kuudraDistance) return;
                    if (kuudraDist < 0) return;
                    var font = Minecraft.getInstance().font;
                    int x = HudManager.x("KuudraDist"), y = HudManager.y("KuudraDist");
                    float s = HudManager.scale("KuudraDist");
                    String color = kuudraDist < 5 ? "§c" : kuudraDist < 10 ? "§e" : "§a";
                    HudManager.drawScaled(context, font,
                            color + String.format("%.1f", kuudraDist) + "m", x, y, s);
                });

        // Ichor pool rendering
        RenderPhaseRegister.register(ctx -> {
            for (var pool : ichorPools) {
                WorldRenderUtils.drawCircle(ctx, pool.center.x, pool.center.y, pool.center.z,
                        ICHOR_RADIUS, 0, 1, 1, 0.5f, true, 3f);
            }
        });
    }

    // Public for HUD
    public static float getKuudraDistance() { return kuudraDist; }

    private static String formatRendDamage(float d) {
        if (d >= 1_000_000) return String.format("%.2fM", d / 1_000_000);
        if (d >= 1_000) return String.format("%.2fK", d / 1_000);
        return String.format("%.0f", d);
    }
}
