package top.babyzombie.addons.module.garden;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.BeaconStateInjector;
import top.babyzombie.addons.util.HypixelLocationTracker;

import java.awt.Color;

public final class PestDisplay {
    private PestDisplay() {}

    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (!ModConfigManager.get().garden.pestDisplay) return;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock() || !"Garden".equals(tracker.getMap())) return;
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            player.level().getEntities(player, new AABB(player.blockPosition()).inflate(48),
                    e -> e instanceof LivingEntity le && e != player && !(e instanceof net.minecraft.world.entity.player.Player)
                            && le.getMaxHealth() >= 600 && le.getMaxHealth() <= 1200)
                    .forEach(e -> BeaconStateInjector.addBeam(
                        e.getX() - 0.5, e.getY() + 1, e.getZ() - 0.5,
                        new Color(0, 255, 0, 180), 300f));
        });
    }
}
