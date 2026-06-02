package top.babyzombie.addons.module.mining;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.WorldTextRenderer;
import top.babyzombie.addons.util.WorldTextRenderer.TextEntry;

public final class ChestMarkers {
    private static final List<BlockPos> chests = new ArrayList<>();

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
                        if (level.getBlockState(bp).is(Blocks.CHEST)) {
                            chests.add(bp);
                        }
                    }
                }
            }
        });

        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (!ModConfigManager.get().mining.chestMarkers) return;
            if (!isInCrystalHollows() || chests.isEmpty()) return;

            var entries = new ArrayList<TextEntry>();
            for (var pos : chests) {
                entries.add(new TextEntry("§cChest", pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0xFF5555));
            }
            WorldTextRenderer.render(ctx.matrices(), entries);
        });
    }

    private static boolean isInCrystalHollows() {
        var t = HypixelLocationTracker.getInstance();
        return t.isInSkyblock() && "Crystal Hollows".equals(t.getLocation());
    }
}
