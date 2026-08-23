package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.render.BeamRenderer;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.render.RenderPhaseRegister;

/**
 * Voidgloom Seraph 信标的可视化辅助：
 * <ul>
 *   <li>信标被扔出在空中（戴在盔甲架头上）时，深度测试发光盔甲架的帽子（选择性 HEAD 槽位发光）；</li>
 *   <li>信标落地成方块后，在其位置绘制一束信标光柱。</li>
 * </ul>
 * 状态直接读取 {@link SlayerBossDetector#voidgloom}，不重复解析世界。
 */
public final class VoidgloomBeaconEffects {
    private VoidgloomBeaconEffects() {}

    /** 上一次正在发光的盔甲架，用于状态变化/功能关闭时主动清除 */
    private static Entity lastGlowEntity;

    public static void init() {
        // 空中信标发光：每 tick 对照状态机，diff 式开关发光
        ClientTickEvents.END_CLIENT_TICK.register(VoidgloomBeaconEffects::tick);

        // 落地信标光柱：世界渲染阶段绘制
        RenderPhaseRegister.register(ctx -> {
            var cfg = ModConfigManager.get().slayer.slayerBossInfo.voidgloom;
            var vg = SlayerBossDetector.voidgloom;
            if (!cfg.beaconBeam) return;
            if (!"onTheGround".equals(vg.beaconStatus) || vg.beaconLoc == null) return;

            BeamRenderer.drawBeam(ctx,
                vg.beaconLoc.getX() + 0.5, vg.beaconLoc.getY(), vg.beaconLoc.getZ() + 0.5,
                2048, 0.15f, cfg.beaconBeamColor.getEffectiveColourRGB());
        });
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            clearGlow();
            return;
        }
        var cfg = ModConfigManager.get().slayer.slayerBossInfo.voidgloom;
        Entity target = null;
        if (cfg.beaconGlow) {
            var vg = SlayerBossDetector.voidgloom;
            if ("§ethrown".equals(vg.beaconStatus) && vg.beaconEntity != null && vg.beaconEntity.isAlive()) {
                target = vg.beaconEntity;
            }
        }

        if (target != null) {
            if (lastGlowEntity != target) {
                clearGlow();
                // 深度测试发光，只高亮盔甲架帽子上戴的信标
                GlowController.setGlowSlots(target, true,
                        cfg.beaconGlowColor.getEffectiveColourRGB(), true, EquipmentSlot.HEAD);
                lastGlowEntity = target;
            }
        } else {
            clearGlow();
        }
    }

    private static void clearGlow() {
        if (lastGlowEntity != null) {
            GlowController.setGlow(lastGlowEntity, false);
            lastGlowEntity = null;
        }
    }
}