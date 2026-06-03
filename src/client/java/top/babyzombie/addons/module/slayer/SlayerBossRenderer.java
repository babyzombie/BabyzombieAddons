package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.BeaconBeamRenderer;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.WorldRenderUtils;

import java.awt.Color;

/**
 * Renders glow effect and beacon beams on slayer bosses.
 */
public final class SlayerBossRenderer {
    private SlayerBossRenderer() {}

    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            var cfg = ModConfigManager.get().slayer;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

            // --- Boss glow ---
            applyBossGlow(cfg.boxslayerboss);

            // --- Boss beam (GLOW_AND_BEAM only) ---
            boolean beam = cfg.boxslayerboss == ModConfig.BoxSlayerMode.GLOW_AND_BEAM;
            var boss = BossDetector.currentBoss;
            if (beam && boss != null && !boss.isDeadOrDying()) {
                BeaconBeamRenderer.render(boss.getX() + 0.5, boss.getY(), boss.getZ() + 0.5,
                    cfg.boxbosscolor, BeaconBeamRenderer.DEFAULT_HEIGHT);
            }

            // --- Voidgloom beacon beam ---
            if (BossDetector.beacon.loc != null) {
                var bp = BossDetector.beacon.loc;
                BeaconBeamRenderer.render(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5,
                    new Color(255, 255, 255, 128), BeaconBeamRenderer.DEFAULT_HEIGHT);
            }

            // --- Inferno split mobs ---
            for (var mob : BossDetector.infernoMobs) {
                if (mob == null || mob.isDeadOrDying()) continue;
                applyGlowToEntity(mob, cfg.boxslayerboss);
                if (beam) {
                    BeaconBeamRenderer.render(mob.getX() + 0.5, mob.getY(), mob.getZ() + 0.5,
                        cfg.boxbosscolor, BeaconBeamRenderer.DEFAULT_HEIGHT);
                }
            }

            // --- Low HP Bloodfiend marker ---
            if (cfg.boxLowHPBloodfiend) {
                var tracker = HypixelLocationTracker.getInstance();
                if ("Stillgore Château".equals(tracker.getLocation())) {
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        for (Player p : player.level().players()) {
                            if (p.isDeadOrDying()) continue;
                            if (!p.getName().getString().equals("Bloodfiend ")) continue;
                            float hpPct = p.getHealth() / p.getMaxHealth();
                            if (hpPct <= 0.2f) {
                                double w = p.getBbWidth(), h = p.getBbHeight();
                                WorldRenderUtils.drawBoxAtEntity(
                                    p.getX(), p.getY(), p.getZ(), w, h, w,
                                    0.48f, 0.41f, 0.93f, 0.8f);
                            }
                        }
                    }
                }
            }
        });
    }

    private static void applyBossGlow(ModConfig.BoxSlayerMode mode) {
        var boss = BossDetector.currentBoss;
        if (boss == null || boss.isDeadOrDying()) {
            removeGlowFromBoss();
            return;
        }
        applyGlowToEntity(boss, mode);
    }

    private static void applyGlowToEntity(LivingEntity entity, ModConfig.BoxSlayerMode mode) {
        boolean wantGlow = mode != ModConfig.BoxSlayerMode.OFF;
        boolean hasGlow = entity.hasEffect(MobEffects.GLOWING);
        if (wantGlow && !hasGlow) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                Integer.MAX_VALUE, 0, true, false));
        } else if (!wantGlow && hasGlow) {
            entity.removeEffect(MobEffects.GLOWING);
        }
    }

    private static void removeGlowFromBoss() {
        var boss = BossDetector.currentBoss;
        if (boss != null && boss.hasEffect(MobEffects.GLOWING)) {
            boss.removeEffect(MobEffects.GLOWING);
        }
    }
}
