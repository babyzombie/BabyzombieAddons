package top.babyzombie.addons.module.mining;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.WorldRenderUtils;

public final class ChestMarkers {
    private static final List<BlockPos> chests = new ArrayList<>();
    private static final float R = 0.2f, G = 1f, B = 0.2f, A = 0.8f;

    private ChestMarkers() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().mining.chestMarkers) return;
            if (!isInCrystalHollows()) return;
            if (client.player == null || client.player.tickCount % 20 != 0) return;

            chests.clear();
            var playerPos = client.player.blockPosition();
            int range = 16;
            var level = client.player.level();
            for (int dx = -range; dx <= range; dx++) {
                for (int dy = -4; dy <= 4; dy++) {
                    for (int dz = -range; dz <= range; dz++) {
                        var bp = playerPos.offset(dx, dy, dz);
                        var bs = level.getBlockState(bp);
                        if (bs.is(Blocks.CHEST) || bs.is(Blocks.TRAPPED_CHEST)) {
                            chests.add(bp);
                        }
                    }
                }
            }
        });

        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (!ModConfigManager.get().mining.chestMarkers) return;
            if (!isInCrystalHollows() || chests.isEmpty()) return;

            double e = 0.02;
            for (var pos : chests) {
                WorldRenderUtils.drawWireframeBox(ctx,
                    pos.getX() - e, pos.getY() - e, pos.getZ() - e,
                    pos.getX() + 1 + e, pos.getY() + 1 + e, pos.getZ() + 1 + e,
                    R, G, B, A, false, ModConfigManager.get().mining.chestLineWidth);
            }
        });
    }

    private static boolean isInCrystalHollows() {
        var t = HypixelLocationTracker.getInstance();
        return t.isInSkyblock() && "Crystal Hollows".equals(t.getMap());
    }
}
