package top.babyzombie.addons.module.garden;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.BeaconBeamRenderer;
import top.babyzombie.addons.util.HypixelLocationTracker;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

/**
 * Shows real-time beacon beam indicators on garden pests.
 * Updates pest positions every 10 ticks and repositions beams dynamically.
 */
public final class PestDisplay {

    private static final Set<String> activeIds = new HashSet<>();
    private static int tickCounter;

    private PestDisplay() {}

    public static void init() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().garden.pestDisplay) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            if (++tickCounter < 1) return;
            tickCounter = 0;

            Set<String> found = new HashSet<>();
            player.level().getEntities(player, new AABB(player.blockPosition()).inflate(48),
                    e -> e instanceof LivingEntity le && e != player && !e.isInvisible()
                            && le.getMaxHealth() >= 600 && le.getMaxHealth() <= 1200)
                    .forEach(e -> {
                        String id = e.getStringUUID();
                        found.add(id);
                        BeaconBeamRenderer.setBeam(id, e.getX(), e.getY() + 1, e.getZ(),
                                new Color(0, 255, 0, 180), BeaconBeamRenderer.DEFAULT_HEIGHT);
                    });

            // Remove beams for pests that disappeared
            activeIds.stream().filter(id -> !found.contains(id))
                    .forEach(BeaconBeamRenderer::removeBeam);
            activeIds.clear();
            activeIds.addAll(found);
        });
    }
}
