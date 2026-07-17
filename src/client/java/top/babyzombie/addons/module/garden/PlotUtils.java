package top.babyzombie.addons.module.garden;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 花园地皮检测工具类。
 *
 * <p>启动时从物品库（skyblocker → firmament → NEU）加载 garden.json，
 * 解析 5×5 网格中每个位置对应的 Hypixel 地皮编号。
 * 坐标→网格行列→地皮编号，三步完成查找。
 */
public final class PlotUtils {

    /// 5×5 网格中每格对应的 Hypixel 地皮编号（1-24），-1 表示无地皮
    /// 索引：gridIds[col][row]
    private static final int[][] gridIds = new int[Plot.GRID_SIZE][Plot.GRID_SIZE];
    private static boolean loaded;

    static {
        ensureLoaded();
    }

    private PlotUtils() {
        throw new UnsupportedOperationException("utility class");
    }

    // ─── 加载 ──────────────────────────────────────────────

    /**
     * 确保已从物品库加载地皮数据。幂等操作，加载失败后下次调用会重试。
     *
     * @return 是否加载成功
     */
    public static boolean ensureLoaded() {
        if (loaded) return true;

        Path gardenJson = resolveItemRepoConstants().resolve("garden.json");
        if (!Files.exists(gardenJson)) return false;

        try {
            String raw = Files.readString(gardenJson);
            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
            loadGrid(obj);
            loaded = true;
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /// 清理网格为 -1
    private static void clearGrid() {
        for (int col = 0; col < Plot.GRID_SIZE; col++) {
            for (int row = 0; row < Plot.GRID_SIZE; row++) {
                gridIds[col][row] = -1;
            }
        }
    }

    /// 从 JSON 的 "plots" 节解析网格
    private static void loadGrid(JsonObject root) {
        clearGrid();
        var plots = root.getAsJsonObject("plots");
        if (plots == null) return;

        for (var entry : plots.entrySet()) {
            JsonObject plot = entry.getValue().getAsJsonObject();
            int col = plot.get("x").getAsInt();
            int row = plot.get("y").getAsInt();
            String rawName = plot.get("name").getAsString();

            // 从 "§aPlot §b2" 中提取数字
            String clean = rawName.replaceAll("§.", "");
            int id = Integer.parseInt(clean.replace("Plot ", ""));

            if (col >= 0 && col < Plot.GRID_SIZE && row >= 0 && row < Plot.GRID_SIZE) {
                gridIds[col][row] = id;
            }
        }
    }

    /// 解析物品库根路径，三路回退：skyblocker → firmament → NEU
    private static Path resolveItemRepoConstants() {
        Path gameDir = FabricLoader.getInstance().getGameDir();

        Path p = gameDir.resolve("config").resolve("skyblocker").resolve("item-repo").resolve("constants");
        if (Files.isDirectory(p)) return p;

        p = gameDir.resolve(".firmament").resolve("repo-extracted").resolve("constants");
        if (Files.isDirectory(p)) return p;

        p = gameDir.resolve("config").resolve("notenoughupdates").resolve("repo").resolve("constants");
        if (Files.isDirectory(p)) return p;

        return gameDir.resolve("config").resolve("skyblocker").resolve("item-repo").resolve("constants");
    }

    // ─── 查询 ──────────────────────────────────────────────

    /**
     * 根据网格行列查 Hypixel 地皮编号（不返回 Plot 对象）。
     *
     * @param col 列号（0-4，X 轴方向）
     * @param row 行号（0-4，Z 轴方向）
     * @return 地皮编号（1-24），中心位置或无数据返回 -1
     */
    public static int idAtGrid(int col, int row) {
        if (col < 0 || col >= Plot.GRID_SIZE || row < 0 || row >= Plot.GRID_SIZE) return -1;
        return gridIds[col][row];
    }

    /**
     * 根据网格行列获取地皮。
     *
     * @param col 列号（0-4，X 轴方向）
     * @param row 行号（0-4，Z 轴方向）
     * @return 对应的 {@link Plot}；行列越界或中心位置返回 {@code null}
     */
    @Nullable
    public static Plot getPlotByGrid(int col, int row) {
        int id = idAtGrid(col, row);
        if (id < 0) {
            if (col == 2 && row == 2) id = 0;
            else return null;
        }
        return buildPlot(id, col, row);
    }

    /**
     * 根据 Hypixel 地皮编号反查地皮。
     *
     * @param id 地皮编号（1-24）
     * @return 对应的 {@link Plot}；编号不存在或数据未加载返回 {@code null}
     */
    @Nullable
    public static Plot getPlotById(int id) {
        if (id < 1 || id > 24) return null;
        for (int col = 0; col < Plot.GRID_SIZE; col++) {
            for (int row = 0; row < Plot.GRID_SIZE; row++) {
                if (gridIds[col][row] == id) {
                    return buildPlot(id, col, row);
                }
            }
        }
        return null;
    }

    /**
     * 根据 XZ 坐标查找地皮。
     *
     * @param x X 坐标
     * @param z Z 坐标
     * @return 对应的 {@link Plot}；如果 XZ 不在花园范围内则返回 {@code null}
     */
    @Nullable
    public static Plot getPlot(double x, double z) {
        if (x < Plot.GARDEN_MIN_X || x > Plot.GARDEN_MAX_X
                || z < Plot.GARDEN_MIN_Z || z > Plot.GARDEN_MAX_Z) {
            return null;
        }
        return plotAt((int) Math.floor(x), (int) Math.floor(z));
    }

    /**
     * 获取当前客户端玩家所在的地皮。
     *
     * @return 玩家所在的 {@link Plot}；如果玩家不在线或不在花园范围内则返回 {@code null}
     */
    @Nullable
    public static Plot getCurrentPlot() {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        return getPlot(player.getX(), player.getZ());
    }

    // ─── 内部实现 ──────────────────────────────────────────

    /// 由 int 坐标计算地皮（内部使用，前置已做边界检查）
    private static Plot plotAt(int blockX, int blockZ) {
        int col = (blockX - Plot.GARDEN_MIN_X) / Plot.PLOT_SIZE;
        int row = (blockZ - Plot.GARDEN_MIN_Z) / Plot.PLOT_SIZE;

        col = Math.clamp(col, 0, Plot.GRID_SIZE - 1);
        row = Math.clamp(row, 0, Plot.GRID_SIZE - 1);

        int id = gridIds[col][row];
        // gridIds 未初始化（-1）且不是谷仓中心 → null；谷仓中心 → id=0
        if (id < 0) {
            if (col == 2 && row == 2) id = 0;
            else return null;
        }

        return buildPlot(id, col, row);
    }

    /// 根据 id + 网格位置构造 Plot 对象
    private static Plot buildPlot(int id, int col, int row) {
        int minX = Plot.GARDEN_MIN_X + col * Plot.PLOT_SIZE;
        int maxX = minX + Plot.PLOT_SIZE - 1;
        int minZ = Plot.GARDEN_MIN_Z + row * Plot.PLOT_SIZE;
        int maxZ = minZ + Plot.PLOT_SIZE - 1;

        return new Plot(id, col, row, minX, maxX,
                Plot.GARDEN_BASE_Y, Plot.GARDEN_TOP_Y, minZ, maxZ);
    }
}
