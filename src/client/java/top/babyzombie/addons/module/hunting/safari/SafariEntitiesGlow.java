package top.babyzombie.addons.module.hunting.safari;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.HashSet;
import java.util.Set;

/**
 * 在 Safari 区域高亮：
 * - 潜影贝（自定义颜色）
 * - Hideyho NPC（淡蓝色）
 * - 较频幽匿感测体（紫色方块发光）
 */
public final class SafariEntitiesGlow {

    private static final String HIDEYHO_NAME = "Hideyho ";
    private static final int HIDEYHO_COLOR = 0xFF80D8FF;
    private static final int SCULK_SENSOR_RANGE = 32;
    private static final int SCULK_SENSOR_RANGE_SQ = SCULK_SENSOR_RANGE * SCULK_SENSOR_RANGE;

    // Warden 战斗场地范围
    private static final int ARENA_X_MIN = -18, ARENA_X_MAX = 24;
    private static final int ARENA_Y_MIN = 45, ARENA_Y_MAX = 62;
    private static final int ARENA_Z_MIN = -39, ARENA_Z_MAX = -13;

    /** 上一次 tick 高亮的较频幽匿感测体位置 */
    private static final Set<BlockPos> sculkSensorHighlighted = new HashSet<>();

    private SafariEntitiesGlow() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;
            if (!HypixelLocationTracker.getInstance().isInSafari()) return;

            var cfg = ModConfigManager.get().hunting;
            boolean glowShulker = cfg.safari.shulkerGlow;
            boolean glowHideyho = cfg.safari.hideyhoGlow;
            boolean glowSculkSensor = cfg.safari.sculkSensorGlow;

            // === 实体发光 ===
            if (glowShulker || glowHideyho) {
                for (var entity : client.level.entitiesForRendering()) {
                    if (glowShulker) {
                        int argb = cfg.safari.shulkerGlowColor.getEffectiveColourRGB();
                        if (entity instanceof Shulker shulker) {
                            GlowController.setGlow(shulker, true, argb, true);
                        } else if (entity instanceof Display.ItemDisplay itemDisplay
                                && BuiltInRegistries.ITEM.getKey(itemDisplay.getItemStack().getItem())
                                    .getPath().contains("shulker_box")) {
                            GlowController.setGlow(itemDisplay, true, argb, true);
                        }
                    }
                    if (glowHideyho && entity instanceof Player player
                            && HIDEYHO_NAME.equals(player.getName().getString())) {
                        GlowController.setGlow(player, true, HIDEYHO_COLOR, true);
                    }
                }
            }

            // === 较频幽匿感测体方块发光 ===
            if (glowSculkSensor && isInArena(client.player.blockPosition())) {
                int sculkColor = cfg.safari.sculkSensorGlowColor.getEffectiveColourRGB();
                var level = client.level;
                var playerPos = client.player.blockPosition();
                int chunkX = playerPos.getX() >> 4;
                int chunkZ = playerPos.getZ() >> 4;
                int chunkR = (SCULK_SENSOR_RANGE >> 4) + 1;

                Set<BlockPos> found = new HashSet<>();

                for (int dcx = -chunkR; dcx <= chunkR; dcx++) {
                    for (int dcz = -chunkR; dcz <= chunkR; dcz++) {
                        int cx = chunkX + dcx;
                        int cz = chunkZ + dcz;
                        BlockPos chunkOrigin = new BlockPos(cx << 4, playerPos.getY(), cz << 4);

                        if (!level.isLoaded(chunkOrigin)) continue;

                        var chunk = level.getChunkAt(chunkOrigin);
                        for (var entry : chunk.getBlockEntities().entrySet()) {
                            if (entry.getValue() instanceof CalibratedSculkSensorBlockEntity) {
                                BlockPos pos = entry.getKey();
                                if (pos.distSqr(playerPos) <= SCULK_SENSOR_RANGE_SQ) {
                                    found.add(pos.immutable());
                                }
                            }
                        }
                    }
                }

                // Diff：移除
                var iter = sculkSensorHighlighted.iterator();
                while (iter.hasNext()) {
                    var pos = iter.next();
                    if (!found.contains(pos)) {
                        GlowController.setBlockGlow(level, pos, false);
                        iter.remove();
                    }
                }

                // Diff：新增
                for (var pos : found) {
                    if (!sculkSensorHighlighted.contains(pos)) {
                        GlowController.setBlockGlow(level, pos, true, sculkColor, true);
                        sculkSensorHighlighted.add(pos);
                    }
                }
            }
        });
    }

    private static boolean isInArena(BlockPos pos) {
        return pos.getX() >= ARENA_X_MIN && pos.getX() <= ARENA_X_MAX
            && pos.getY() >= ARENA_Y_MIN && pos.getY() <= ARENA_Y_MAX
            && pos.getZ() >= ARENA_Z_MIN && pos.getZ() <= ARENA_Z_MAX;
    }
}
