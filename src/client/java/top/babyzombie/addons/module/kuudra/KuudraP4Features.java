package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.render.WorldRenderUtils;
import top.babyzombie.addons.util.render.WorldTextRenderer;
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
    /** 同一笔血量落差（高点/低点）视为同一次掉血事件，只上报一次：防止击杀前后
     *  双形态实体交替被选中时同一数值反复刷屏；多人轮流造成的真实伤害各自不同，不受影响 */
    private static final float REND_DROP_EPSILON = 1f;
    /** bossStartMs 超过该时长视为失效计时（漏匹配 P4 开始消息等），重新锚定 */
    private static final long REND_MAX_BOSS_MS = 600_000;
    private static float lastHP = -1;
    private static long bossStartMs;
    private static float lastDropHigh = -1;
    private static float lastDropLow = -1;

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
        ClientReceiveMessageEvents.ALLOW_GAME.register((msg, overlay) -> {
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return true;

            String text = ChatUtils.stripColor(msg.getString());
            // Reset rend tracking on boss start
            if (KuudraChatLines.isFishUpKuudra(text)) {
                resetRendState();
                ichorPools.clear();
                return true;
            }
            // Start boss timer — T3/T4 及 phaseTimer 关闭时的 fallback 锚点；
            // T5 进入 BOSS 阶段时会在 tick 中覆盖为与 KuudraPhaseTimer 同一起点
            if (KuudraChatLines.isP4Start(text)) {
                bossStartMs = ServerTick.getTime();
            }
            // Boss killed — 清理 Rend 临时状态，避免结束后继续触发
            if (KuudraChatLines.isKuudraDown(text)) {
                resetRendState();
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
                        var level = Minecraft.getInstance().level;
                        if (level == null) return true;
                        while (cy >= -64 && level.getBlockState(new BlockPos(cx, cy - 1, cz)).isAir()) {
                            cy--;
                        };
                        ichorPools.add(new IchorPool(new Vec3(cx + 0.5, cy + 0.05, cz + 0.5),
                                System.currentTimeMillis() + ICHOR_DURATION_MS));
                    } catch (NumberFormatException ignored) {}
                }
            }
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            var cfg = ModConfigManager.get().kuudra.phase4;
            if (client.player == null) return;

            boolean inP4 = KuudraLocationTracker.p4 || "p4".equals(KuudraLocationTracker.area);
            if (!inP4) return;

            // T5 BOSS 阶段锚定：与 KuudraPhaseTimer 的 boss 计时同源同起点（ServerTick 时钟），
            // 让 rend 秒数与 splits 的 BOSS 行一致；phaseTimer 关闭时 currentPhase 恒为 null，
            // 自然回退到 P4_START 消息锚点
            if (KuudraPhaseTimer.currentPhase() == KuudraPhaseTimer.Phase.BOSS
                    && bossStartMs != KuudraPhaseTimer.getPhaseStartTick()) {
                bossStartMs = KuudraPhaseTimer.getPhaseStartTick();
            }

            LivingEntity kuudra = KuudraLocationTracker.kuudraEntity;
            if (kuudra == null || kuudra.isDeadOrDying()) {
                // Fallback: find wither named Kuudra
                var withers = client.player.level().getEntitiesOfClass(
                        net.minecraft.world.entity.boss.wither.WitherBoss.class,
                        new net.minecraft.world.phys.AABB(client.player.blockPosition()).inflate(128),
                        w -> ChatUtils.stripColor(w.getName().getString()).contains("Kuudra"));
                if (!withers.isEmpty()) kuudra = withers.getFirst();
            }
            boolean alive = kuudra != null && !kuudra.isDeadOrDying();

            // Rend tracking — 去掉 hp>0 限制：实体死亡/消失时 hp 视为 0，
            // 打死 Kuudra 那一击（落差 → 0）也要记上
            if (cfg.rendTracker) {
                float hp = alive ? kuudra.getHealth() : 0f;
                if (lastHP > 0 && lastHP - hp > MIN_REND_RAW) {
                    boolean sameDrop = Math.abs(lastDropHigh - lastHP) < REND_DROP_EPSILON
                            && Math.abs(lastDropLow - hp) < REND_DROP_EPSILON;
                    if (!sameDrop) {
                        float scaled = (lastHP - hp) * REND_MULTIPLIER;
                        double sec = rendSeconds();
                        if (sec > 1.2) {
                            // Component.translatable 不支持 %.2f 修饰(所有 % 指令按 %s toString 替换),
                            // 因此时间/伤害必须先格式化好,再作为纯 %s 参数传入
                            ChatUtils.showTranslatable("kuudra.rend",
                                    formatRendSeconds(sec), formatRendDamage(scaled));
                            lastDropHigh = lastHP;
                            lastDropLow = hp;
                        }
                    }
                }
                if (alive) {
                    lastHP = hp;
                } else {
                    // Kuudra 死亡/消失 — 击杀击已在上方记录，清理 Rend 临时状态
                    resetRendState();
                }
            }

            // Distance
            if (cfg.kuudraDistance && alive) {
                kuudraDist = kuudra.distanceTo(client.player);
            }

            // Expire ichor pools
            long now = System.currentTimeMillis();
            ichorPools.removeIf(ichorPool -> ichorPool.expiresAt <= now);
        });

        // Kuudra distance HUD
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_distance"),
                (context, tickCounter) -> {
                    if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
                    if (!ModConfigManager.get().kuudra.phase4.kuudraDistance) return;
                    if (kuudraDist < 0) return;
                    var font = Minecraft.getInstance().font;
                    int x = HudManager.x("KuudraDist"), y = HudManager.y("KuudraDist");
                    float s = HudManager.scale("KuudraDist");
                    String color = kuudraDist < 5 ? "§c" : kuudraDist < 10 ? "§e" : "§a";
                    HudManager.drawScaled(context, font,
                            color + String.format("%.1f", kuudraDist) + "m", x, y, s);
                });

        // Ichor pool rendering（离开 Kuudra 不再渲染）
        RenderPhaseRegister.register(ctx -> {
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            for (var pool : ichorPools) {
                long remaining = pool.expiresAt - System.currentTimeMillis();
                int passed = Math.toIntExact(ICHOR_DURATION_MS - remaining);

                // 地面层范围圈
                WorldRenderUtils.drawCircle(ctx, pool.center.x, pool.center.y, pool.center.z,
                        ICHOR_RADIUS, 0, 1, 1, 0.5f, true, 5f);
                // 空中动态范围圈
                for (int i = 0; i * 1000 < passed; i += 2) {
                    if (passed - i * 1000 > 10000) continue;
                    WorldRenderUtils.drawCircle(ctx, pool.center.x, pool.center.y + (passed - i * 1000.0) / 1000.0, pool.center.z,
                            ICHOR_RADIUS, 0, 1, 1, 0.5f, true, 5f);
                }
                if (passed >= 100000) WorldRenderUtils.drawCircle(ctx, pool.center.x, pool.center.y + 10, pool.center.z,
                        ICHOR_RADIUS, 0, 1, 1, 0.5f, true, 5f);

                // 中心悬浮字：名称 + 剩余时间倒计时
                if (remaining <= 0) continue;
                WorldTextRenderer.renderString(ctx, "§bIchor Pool",
                        pool.center.x, pool.center.y + 1.2, pool.center.z, 0xFF55FFFF, 0.08f, true);
                WorldTextRenderer.renderString(ctx, String.format("§f%.1fs", remaining / 1000.0),
                        pool.center.x, pool.center.y + 0.2, pool.center.z, 0xFFFFFFFF, 0.06f, true);
            }
        });
    }

    // Public for HUD
    public static float getKuudraDistance() { return kuudraDist; }

    /**
     * 返回距 BOSS 阶段开始的秒数（与 KuudraPhaseTimer 的 boss 计时同源）；计时无效
     * （未收到 P4 开始消息 / 未进入 BOSS 阶段 / 超过上限）时重新锚定到当前时间，
     * 返回 0 让本次掉血不触发，避免出现 epoch 巨数或跨越多场战斗的时间。
     */
    private static double rendSeconds() {
        long now = ServerTick.getTime();
        if (bossStartMs == 0 || now - bossStartMs > REND_MAX_BOSS_MS) {
            bossStartMs = now;
            return 0;
        }
        return (now - bossStartMs) / 1000.0;
    }

    /** 清理 Rend 追踪临时状态（击杀、实体死亡/消失时调用） */
    private static void resetRendState() {
        lastHP = -1;
        bossStartMs = 0;
        lastDropHigh = -1;
        lastDropLow = -1;
    }

    private static String formatRendSeconds(double sec) {
        return String.format("%.2fs", sec);
    }

    private static String formatRendDamage(float d) {
        if (d >= 1_000_000) return String.format("%.2fM", d / 1_000_000);
        if (d >= 1_000) return String.format("%.2fK", d / 1_000);
        return String.format("%.0f", d);
    }
}
