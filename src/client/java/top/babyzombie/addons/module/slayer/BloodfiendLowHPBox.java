package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.WorldRenderUtils;

public final class BloodfiendLowHPBox {
    private BloodfiendLowHPBox() {}

    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (!ModConfigManager.get().slayer.boxLowHPBloodfiend) return;
            var tracker = HypixelLocationTracker.getInstance();
            if (!"Stillgore Château".equals(tracker.getLocation())) return;
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            for (var p : player.level().players()) {
                if (p.isDeadOrDying()) continue;
                if (!p.getName().getString().equals("Bloodfiend ")) continue;
                float hpPct = p.getHealth() / p.getMaxHealth();
                if (hpPct <= 0.2f) {
                    var bb = p.getBoundingBox();
                    WorldRenderUtils.drawFilledBox(ctx,
                        bb.minX, bb.minY, bb.minZ,
                        bb.maxX, bb.maxY, bb.maxZ,
                        0.55f, 0.35f, 0.85f, 0.45f, true);
                }
            }
        });
    }
}
