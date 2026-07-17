package top.babyzombie.addons.module.garden;

import net.minecraft.world.phys.AABB;

/**
 * 花园地皮对象，存储 Hypixel 地皮编号和坐标区间。
 *
 * <p>花园为 5×5 网格，每块 96×96 格，共 24 块地皮（编号 1-24），中心 (2,2) 为谷仓区域无地皮。
 * X/Z 坐标范围：-240 ~ 239，Y 坐标范围：67 ~ 86（基岩在 66）。
 *
 * @param id   地皮编号（1-24，Hypixel 花园 Plot 编号）
 * @param col  列号（0-4，X 轴方向）
 * @param row  行号（0-4，Z 轴方向）
 * @param minX X 轴最小坐标（含）
 * @param maxX X 轴最大坐标（含）
 * @param minY 花园最小 Y 坐标（67）
 * @param maxY 花园最大 Y 坐标（86）
 * @param minZ Z 轴最小坐标（含）
 * @param maxZ Z 轴最大坐标（含）
 */
public record Plot(
        int id,
        int col,
        int row,
        int minX,
        int maxX,
        int minY,
        int maxY,
        int minZ,
        int maxZ
) {

    // ─── 花园全局常量 ──────────────────────────────────────

    /// X/Z 轴总格数：239 - (-240) + 1 = 480
    public static final int TOTAL_SIZE = 480;
    /// 5×5 网格
    public static final int GRID_SIZE = 5;
    /// 每块地皮边长：480 / 5 = 96 格
    public static final int PLOT_SIZE = TOTAL_SIZE / GRID_SIZE;

    /// 花园 X 轴最小/最大坐标（含）
    public static final int GARDEN_MIN_X = -240;
    public static final int GARDEN_MAX_X = 239;
    /// 花园 Z 轴最小/最大坐标（含）
    public static final int GARDEN_MIN_Z = -240;
    public static final int GARDEN_MAX_Z = 239;

    /// 花园 Y 轴基准高度（基岩在 66）
    public static final int GARDEN_BASE_Y = 67;
    /// 花园高度（格）
    public static final int GARDEN_HEIGHT = 20;
    /// 花园 Y 轴最大坐标（含）：67 + 20 - 1 = 86
    public static final int GARDEN_TOP_Y = GARDEN_BASE_Y + GARDEN_HEIGHT - 1;

    // ─── 便捷方法 ──────────────────────────────────────────

    /**
     * @return 该地皮中心 X 坐标
     */
    public double centerX() {
        return (minX + maxX) / 2.0;
    }

    /**
     * @return 该地皮中心 Z 坐标
     */
    public double centerZ() {
        return (minZ + maxZ) / 2.0;
    }

    /**
     * @param x X 坐标
     * @param z Z 坐标
     * @return 该坐标是否在此地皮范围内（仅 XZ 平面）
     */
    public boolean contains(double x, double z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /**
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @return 该坐标是否在此地皮范围内（含 Y 轴）
     */
    public boolean contains(double x, double y, double z) {
        return contains(x, z) && y >= minY && y <= maxY;
    }

    /**
     * @return 该地皮的轴对齐包围盒
     */
    public AABB aabb() {
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }
}
