package top.babyzombie.addons.module.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.EntityRenderEvents;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * Hides all entities within a configurable range around the player.
 * The player themselves, their fishing bobber and their mount are exempt by default.
 */
public final class EntityHider {

    private EntityHider() {}

    public static void init() {
        EntityRenderEvents.BEFORE_RENDER.register(entity -> {
            var cfg = ModConfigManager.get().general.entityHider;
            if (!cfg.enabled) return false;
            if (cfg.skyblockOnly && !HypixelLocationTracker.getInstance().isInSkyblock()) return false;

            var player = Minecraft.getInstance().player;
            if (player == null) return false;

            // 永远不隐藏自己(第三视角下仍能看到自己的模型)
            if (entity == player) return false;
            // 自己的钓鱼浮标:默认保留,否则钓鱼时看不到浮标
            if (!cfg.hideOwnBobber && entity instanceof FishingHook hook && hook.getOwner() == player) return false;
            // 自己的坐骑:默认保留,否则会骑在空气上
            if (!cfg.hideOwnMount && entity.getPassengers().contains(player)) return false;

            double range = cfg.range;
            return entity.distanceToSqr(player) <= range * range;
        });
    }
}
