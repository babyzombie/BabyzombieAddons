package top.babyzombie.addons.util.render;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import top.babyzombie.addons.mixin.render.ClientLevelAccessor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GlowController {
    private GlowController() {}

    // ==================== 实体发光 ====================
    private static final Map<UUID, Integer> GLOW_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> DEPTH_TEST = new ConcurrentHashMap<>();

    // ==================== 方块发光 ====================
    /** 方块位置 → BlockDisplay 实体映射，entity 的 glow 状态仍然走 GLOW_DATA/DEPTH_TEST */
    private static final Map<BlockPos, Display.BlockDisplay> BLOCK_GLOWS = new ConcurrentHashMap<>();

    static {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((_, _) -> clearAll());
    }

    /** Just enable glow, keep existing or default white. */
    public static void setGlow(Entity entity) {
        setGlow(entity, true);
    }

    /** Toggle glow on/off. If no color was set, defaults to white. */
    public static void setGlow(Entity entity, boolean glow) {
        Integer existing = GLOW_DATA.get(entity.getUUID());
        setGlow(entity, glow, existing != null ? existing : 0xFFFFFF);
    }

    /** Toggle glow on/off with a specific color. */
    public static void setGlow(Entity entity, boolean glow, int color) {
        Boolean depthTest = DEPTH_TEST.get(entity.getUUID());
        setGlow(entity, glow, color, depthTest != null && depthTest);
    }

    /** Toggle glow on/off with a specific color and optional depth test. */
    public static void setGlow(Entity entity, boolean glow, int color, boolean depthTest) {
        UUID uuid = entity.getUUID();
        if (glow) {
            GLOW_DATA.put(uuid, color);
            if (depthTest) {
                DEPTH_TEST.put(uuid, true);
            } else {
                DEPTH_TEST.remove(uuid);
            }
        } else {
            GLOW_DATA.remove(uuid);
            DEPTH_TEST.remove(uuid);
        }
    }

    public static boolean shouldGlow(Entity entity) {
        return GLOW_DATA.containsKey(entity.getUUID());
    }

    public static int getGlowColor(Entity entity) {
        Integer color = GLOW_DATA.get(entity.getUUID());
        return color != null ? color : 0xFFFFFF;
    }

    /** 检查特定实体是否启用了深度测试发光。 */
    public static boolean isDepthTestEnabled(Entity entity) {
        return DEPTH_TEST.getOrDefault(entity.getUUID(), false);
    }

    /** 检查当前是否有任何实体需要深度测试发光。 */
    public static boolean isAnyDepthTestRequested() {
        return !DEPTH_TEST.isEmpty();
    }

    /** 清除所有发光数据（世界卸载时自动调用）。 */
    public static void clearAll() {
        // 先清理方块发光 entity
        for (var display : BLOCK_GLOWS.values()) {
            display.remove(Entity.RemovalReason.DISCARDED);
        }
        BLOCK_GLOWS.clear();
        GLOW_DATA.clear();
        DEPTH_TEST.clear();
    }

    // ==================== 方块发光 API ====================

    /**
     * 设置方块发光。幂等：已发光则更新颜色和深度测试状态。
     *
     * @param level     当前客户端世界
     * @param pos       方块位置
     * @param glow      是否发光
     * @param color     RGB 颜色 (0xRRGGBB)
     * @param depthTest 是否启用深度测试（false = 穿墙可见）
     */
    public static void setBlockGlow(ClientLevel level, BlockPos pos, boolean glow, int color, boolean depthTest) {
        if (glow) {
            pos = pos.immutable();
            var display = BLOCK_GLOWS.get(pos);

            if (display == null || display.isRemoved()) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) return;

                display = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level);
                display.setPos(pos.getX(), pos.getY(), pos.getZ());
                display.setBlockState(state);
                display.setInvisible(true);

                // 绕中心微缩放，避免轮廓和原方块 Z-fighting
                display.setTransformation(new com.mojang.math.Transformation(
                        new org.joml.Vector3f(-0.001f, -0.001f, -0.001f),
                        null,
                        new org.joml.Vector3f(1.002f, 1.003f, 1.002f),
                        null
                ));

                ((ClientLevelAccessor) level).getEntityStorage().addEntity(display);
                BLOCK_GLOWS.put(pos, display);
            } else {
                // 原方块可能变化（如作物生长），同步更新
                BlockState current = level.getBlockState(pos);
                if (!current.equals(display.getBlockState())) {
                    display.setBlockState(current);
                }
            }

            setGlow(display, true, color, depthTest);
        } else {
            var display = BLOCK_GLOWS.remove(pos);
            if (display != null) {
                setGlow(display, false);
                display.remove(Entity.RemovalReason.DISCARDED);
            }
        }
    }

    /** 设置方块发光，深度测试默认保持上次设置。 */
    public static void setBlockGlow(ClientLevel level, BlockPos pos, boolean glow, int color) {
        var display = BLOCK_GLOWS.get(pos);
        boolean depthTest = display != null && isDepthTestEnabled(display);
        setBlockGlow(level, pos, glow, color, depthTest);
    }

    /** 开关方块发光，颜色默认白色。 */
    public static void setBlockGlow(ClientLevel level, BlockPos pos, boolean glow) {
        setBlockGlow(level, pos, glow, 0xFFFFFF);
    }

    /** 查询指定位置是否有方块发光。 */
    public static boolean shouldBlockGlow(BlockPos pos) {
        var display = BLOCK_GLOWS.get(pos);
        return display != null && !display.isRemoved() && shouldGlow(display);
    }

    /** 获取方块发光颜色。 */
    public static int getBlockGlowColor(BlockPos pos) {
        var display = BLOCK_GLOWS.get(pos);
        return display != null ? getGlowColor(display) : 0xFFFFFF;
    }

    /** 获取正在发光的方块位置集合（用于调用方 diff 更新）。 */
    public static java.util.Set<BlockPos> getGlowingBlocks() {
        return Map.copyOf(BLOCK_GLOWS).keySet();
    }
}
