package top.babyzombie.addons.module.dungeon.withercloak;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.monster.Creeper;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.EntityRenderEvents;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.Scheduler;

/**
 * Hides invisible charged creepers within 2 blocks when Wither Cloak is active.
 */
public final class ChargedCreeperHider {
    private static final Set<Creeper> nearbyChargedCreepers = Collections.synchronizedSet(new HashSet<>());
    private static boolean wasActive;

    private ChargedCreeperHider() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().dungeon.witherCloak.hideChargedCreepers) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

            boolean active = WitherCloakTimer.active;
            if (active != wasActive) {
                if (active) {
                    nearbyChargedCreepers.clear();
                } else {
                    Scheduler.schedule(4, nearbyChargedCreepers::clear);
                }
                wasActive = active;
            }

            // 定期清理已死亡或消失的 Creeper，防止 Set 无限增长
            if (active && client.player != null && client.player.tickCount % 20 == 0) {
                nearbyChargedCreepers.removeIf(c -> !c.isAlive() || c.isRemoved());
            }
        });

        EntityRenderEvents.BEFORE_RENDER.register(entity -> {
            if (!(entity instanceof Creeper c)) return false;
            if (nearbyChargedCreepers.contains(c)) return true;
            if (!WitherCloakTimer.active) return false;

            var player = Minecraft.getInstance().player;
            if (player == null) return false;
            if (!c.isInvisible()) return false;
            if (!c.isPowered()) return false;
            if (c.getHealth() != 20.0F || c.getMaxHealth() != 20.0F) return false;
            if (player.distanceTo(c) > 2.0) return false;

            nearbyChargedCreepers.add(c);
            return true;
        });
    }
}
