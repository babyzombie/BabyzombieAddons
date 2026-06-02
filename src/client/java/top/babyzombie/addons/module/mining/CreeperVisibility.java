package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.HypixelLocationTracker;

public final class CreeperVisibility {
    private CreeperVisibility() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().mining.creeperVisibility) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            if (client.player == null || client.player.tickCount % 20 != 0) return;

            var loc = HypixelLocationTracker.getInstance().getLocation();
            boolean inGunpowder = "Deep Caverns".equals(HypixelLocationTracker.getInstance().getLocation())
                    && "Gunpowder Mines".equals(loc);
            boolean inMist = "Dwarven Mines".equals(HypixelLocationTracker.getInstance().getLocation())
                    && "The Mist".equals(loc);

            if (!inGunpowder && !inMist) return;

            var creepers = client.player.level().getEntitiesOfClass(Creeper.class,
                    new AABB(client.player.blockPosition()).inflate(64));
            for (var c : creepers) {
                if (c.isInvisible()) c.setInvisible(false);
            }
        });
    }
}
