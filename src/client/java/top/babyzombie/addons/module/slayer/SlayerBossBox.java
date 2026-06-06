package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.world.entity.Entity;
import top.babyzombie.addons.config.ModConfig.SlayerBossBoxMode;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.BeaconStateInjector;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.WorldRenderUtils;

public final class SlayerBossBox {
    private SlayerBossBox() {}

    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            var cfg = ModConfigManager.get().slayer;
            if (cfg.boxSlayerBoss == SlayerBossBoxMode.OFF) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

            Entity boss = SlayerBossDetector.bossEntity;
            if (boss == null) return;

            var def = SlayerBossDetector.BOSS_DEFS.get(SlayerBossDetector.slayerType);
            if (def == null) return;

            boolean depthTest = !cfg.boxBossRenderThroughWalls;
            boolean filled = cfg.boxSlayerBoss == SlayerBossBoxMode.BOX;

            int color = cfg.boxBossColor;
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;
            float a = ((color >> 24) & 0xFF) / 255f;

            double x1 = boss.getX() - def.wX / 2;
            double y1 = boss.getY();
            double z1 = boss.getZ() - def.wZ / 2;
            double x2 = boss.getX() + def.wX / 2;
            double y2 = boss.getY() + def.h;
            double z2 = boss.getZ() + def.wZ / 2;

            // Wireframe box
            WorldRenderUtils.drawWireframeBox(ctx, x1, y1, z1, x2, y2, z2, r, g, b, a, depthTest, 5.0f);

            // Filled box
            if (filled) {
                WorldRenderUtils.drawFilledBox(ctx, x1, y1, z1, x2, y2, z2, r, g, b, a * 0.5f, depthTest);
            }

            // Beacon beam
            if (cfg.boxBossBeam) {
                BeaconStateInjector.addBeam(boss.getX() - 1, boss.getY(), boss.getZ() - 1, cfg.boxBossBeamColor, 2048);
            }

            // Inferno minion boxes
            if (!SlayerBossDetector.infernoMinions.isEmpty()) {
                for (Entity minion : SlayerBossDetector.infernoMinions) {
                    if (minion == null || !minion.isAlive()) continue;
                    double mx1 = minion.getX() - def.wX / 2;
                    double my1 = minion.getY();
                    double mz1 = minion.getZ() - def.wZ / 2;
                    double mx2 = minion.getX() + def.wX / 2;
                    double my2 = minion.getY() + def.h;
                    double mz2 = minion.getZ() + def.wZ / 2;

                    WorldRenderUtils.drawWireframeBox(ctx, mx1, my1, mz1, mx2, my2, mz2, r, g, b, a, depthTest, 5.0f);
                    if (filled) {
                        WorldRenderUtils.drawFilledBox(ctx, mx1, my1, mz1, mx2, my2, mz2, r, g, b, a * 0.5f, depthTest);
                    }
                    if (cfg.boxBossBeam) {
                        BeaconStateInjector.addBeam(minion.getX() - 1, minion.getY(), minion.getZ() - 1, cfg.boxBossBeamColor, 2048);
                    }
                }
            }
        });
    }
}
