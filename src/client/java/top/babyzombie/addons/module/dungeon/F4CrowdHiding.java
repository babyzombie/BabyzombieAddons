package top.babyzombie.addons.module.dungeon;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.EntityRenderEvents;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * Hides or removes spectator crowd entities in the F4 boss room.
 */
public final class F4CrowdHiding {

    private F4CrowdHiding() {}

    public static void init() {
        EntityRenderEvents.BEFORE_RENDER.register(entity -> {
            var mode = ModConfigManager.get().dungeon.f4CrowdHiding;
            if (mode == ModConfig.CrowdHideMode.OFF) return false;
            if (!HypixelLocationTracker.getInstance().isInDungeon()) return false;
            String floor = HypixelLocationTracker.getInstance().getFloor();
            if (floor == null || !floor.contains("4")) return false;

            var player = Minecraft.getInstance().player;
            if (player == null) return false;
            double px = player.getX(), py = player.getY(), pz = player.getZ();
            if (px < -40 || px > 50 || py < 0 || py > 255 || pz < -40 || pz > 50) return false;

            if (entity instanceof net.minecraft.world.entity.player.Player otherPlayer) {
                String name = otherPlayer.getName().getString();
                if (name.contains(" ")) return false;
                if (name.contains("Decoy") || name.contains("Spirit Bear")) return false;
                return mode == ModConfig.CrowdHideMode.HIDE;
            }
            boolean isCrowd = entity.getClass().getSimpleName().equals("Zombie")
                    || entity.getClass().getSimpleName().equals("Skeleton");
            return isCrowd && mode == ModConfig.CrowdHideMode.HIDE;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ModConfigManager.get().dungeon.f4CrowdHiding != ModConfig.CrowdHideMode.REMOVE) return;
            if (!HypixelLocationTracker.getInstance().isInDungeon()) return;
            String floor = HypixelLocationTracker.getInstance().getFloor();
            if (floor == null || !floor.contains("4")) return;
            if (client.player == null || client.level == null) return;
            double px = client.player.getX(), py = client.player.getY(), pz = client.player.getZ();
            if (px < -40 || px > 50 || py < 0 || py > 255 || pz < -40 || pz > 50) return;

            for (var entity : client.level.entitiesForRendering()) {
                if (entity instanceof net.minecraft.world.entity.player.Player otherPlayer) {
                    String name = otherPlayer.getName().getString();
                    if (name.contains(" ") || name.contains("Decoy") || name.contains("Spirit Bear")) continue;
                    entity.discard();
                } else if (entity.getClass().getSimpleName().equals("Zombie")
                        || entity.getClass().getSimpleName().equals("Skeleton")) {
                    entity.discard();
                }
            }
        });
    }
}
