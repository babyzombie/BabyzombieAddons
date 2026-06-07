package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.WorldRenderUtils;

public final class BloodfiendLowHPBox {
    private BloodfiendLowHPBox() {}

    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (!ModConfigManager.get().slayer.boxLowHPBloodfiend) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

            var level = Minecraft.getInstance().level;
            if (level == null) return;

            for (Entity e : level.entitiesForRendering()) {
                if (e.getType() != EntityType.PLAYER) continue;
                if (!e.isAlive()) continue;
                if (!"Bloodfiend ".equals(e.getName().getString())) continue;

                float hpPct = ((LivingEntity) e).getHealth() / ((LivingEntity) e).getMaxHealth();
                if (hpPct <= 0.2f) {
                    var bb = e.getBoundingBox();
                    WorldRenderUtils.drawFilledBox(ctx,
                        bb.minX, bb.minY, bb.minZ,
                        bb.maxX, bb.maxY, bb.maxZ,
                        0.55f, 0.35f, 0.85f, 0.8f, false);
                }
            }
        });
    }
}
