package top.babyzombie.addons.module.hunting.torrhuscanyon;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.config.HuntingConfig.BeeheemothHighlightMode;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.render.BeamRenderer;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * 在 Torrhus Canyon 区域高亮 beeheemoth（缩放 9.0 的蜜蜂）。
 * 支持四种标记模式：关、发光（穿墙）、信标光束、同时。
 */
public final class TorrhusCanyonBeeheemoth {

    private static final String MAP_NAME = "Torrhus Canyon";
    private static final float TARGET_SCALE = 9.0f;
    private static final float SCALE_TOLERANCE = 0.01f;
    private static final double BEAM_HEIGHT = 256;
    private static final float BEAM_HALF_WIDTH = 0.15f;

    @Nullable
    private static Bee currentBee;

    private TorrhusCanyonBeeheemoth() {}

    public static void init() {
        // ── Tick：检测 beeheemoth 并设置发光 ──
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;

            var tracker = HypixelLocationTracker.getInstance();
            var cfg = ModConfigManager.get().hunting.torrhusCanyon;

            boolean inCanyon = tracker.isIn(MAP_NAME);
            BeeheemothHighlightMode mode = cfg.highlightMode;

            if (!inCanyon || mode == BeeheemothHighlightMode.OFF) {
                clearCurrent();
                return;
            }

            boolean wantGlow = mode == BeeheemothHighlightMode.GLOW
                    || mode == BeeheemothHighlightMode.BOTH;

            // 查找缩放 ≈9.0 的蜜蜂
            Bee found = null;
            for (var entity : client.level.entitiesForRendering()) {
                if (entity instanceof Bee bee && isScaleMatch(bee)) {
                    found = bee;
                    break;
                }
            }

            // 切换目标时清除旧发光
            if (currentBee != null && (found == null || !found.equals(currentBee))) {
                GlowController.setGlow(currentBee, false);
                currentBee = null;
            }

            if (found != null) {
                currentBee = found;
                if (wantGlow) {
                    int color = cfg.glowColor.getEffectiveColourRGB();
                    GlowController.setGlow(found, true, color, false); // depthTest=false 穿墙
                } else {
                    GlowController.setGlow(found, false);
                }
            }
        });

        // ── 渲染：信标光束 ──
        RenderPhaseRegister.register(ctx -> {
            if (!HypixelLocationTracker.getInstance().isIn(MAP_NAME)) return;

            var cfg = ModConfigManager.get().hunting.torrhusCanyon;
            BeeheemothHighlightMode mode = cfg.highlightMode;
            if (mode != BeeheemothHighlightMode.BEACON
                    && mode != BeeheemothHighlightMode.BOTH) return;

            Bee bee = currentBee;
            if (bee == null || bee.isRemoved()) return;

            int color = cfg.glowColor.getEffectiveColourRGB();
            BeamRenderer.drawBeam(ctx,
                    bee.getX(), bee.getY() + 2, bee.getZ(),
                    BEAM_HEIGHT, BEAM_HALF_WIDTH, color);
        });
    }

    private static boolean isScaleMatch(LivingEntity entity) {
        return Math.abs(entity.getScale() - TARGET_SCALE) < SCALE_TOLERANCE;
    }

    private static void clearCurrent() {
        if (currentBee != null) {
            GlowController.setGlow(currentBee, false);
            currentBee = null;
        }
    }
}
