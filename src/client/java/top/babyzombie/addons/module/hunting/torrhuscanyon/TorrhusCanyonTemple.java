package top.babyzombie.addons.module.hunting.torrhuscanyon;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.render.WorldRenderContext;
import top.babyzombie.addons.util.render.WorldRenderUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * Torrhus Canyon 地下神庙解密辅助。
 * <p>
 * 第一关：地面彩色玻璃，按从少到多顺序点击对应按钮。<br>
 * 第二关：四个雕像，按食物链顺序调整雕像朝向。
 * <p>
 * 两个谜题的答案都是固定的，直接写死。
 */
public final class TorrhusCanyonTemple {

    private static final String MAP_NAME = "Torrhus Canyon";

    // ═══════════════════════════════════════════════════════════════
    // Puzzle 1 — 彩色玻璃按钮
    // ═══════════════════════════════════════════════════════════════

    /** 门检测：-612 44 234 如果是空气说明门开着，无需解密 */
    private static final BlockPos PUZZLE1_DOOR = new BlockPos(-612, 44, 234);

    /**
     * 12 个按钮在 z=232 墙上的位置，绕中心 (-612, 47) 顺时针排列。
     * 从 5 个已知按钮坐标推导得出。
     */
    private static final BlockPos[] BUTTONS = {
            new BlockPos(-610, 47, 232), //  0: 右
            new BlockPos(-610, 48, 232), //  1: 右上
            new BlockPos(-611, 49, 232), //  2: 上偏右  ← 第4个按
            new BlockPos(-612, 49, 232), //  3: 上      ← 第3个按
            new BlockPos(-613, 49, 232), //  4: 上偏左  ← 第5个按
            new BlockPos(-614, 48, 232), //  5: 左上    ← 第2个按
            new BlockPos(-614, 47, 232), //  6: 左
            new BlockPos(-614, 46, 232), //  7: 左下
            new BlockPos(-613, 45, 232), //  8: 下偏左  ← 第1个按
            new BlockPos(-612, 45, 232), //  9: 下
            new BlockPos(-611, 45, 232), // 10: 下偏右
            new BlockPos(-610, 46, 232), // 11: 右下
    };

    /** 按下的顺序（BUTTONS 数组索引），从少到多 */
    private static final int[] PRESS_ORDER = {8, 5, 3, 2, 4};

    /**
     * 黑石按钮按了后会弹起来（POWERED 只有短暂 ~1 秒），
     * 且可以同时多个按钮处于按下状态，服务器只跟踪按下的顺序。
     * 用 bitmask 追踪所有按钮的 POWERED 状态：
     * <ul>
     * <li>prevPoweredMask：上一 tick 的按钮状态（bit i = 按钮 i 处于 POWERED）</li>
     * <li>areaInitialized：是否已记录进入区域时的初始状态</li>
     * <li>pressProgress：已经正确按下的按钮数（0-5）</li>
     * </ul>
     */
    private static int prevPoweredMask = 0;
    private static boolean areaInitialized = false;
    private static int pressProgress = 0;

    // ═══════════════════════════════════════════════════════════════
    // Puzzle 2 — 雕像食物链
    // ═══════════════════════════════════════════════════════════════

    /** 门检测：-603 46 248 如果是空气说明门开了 */
    private static final BlockPos PUZZLE2_DOOR = new BlockPos(-603, 46, 248);

    /** 雕像高度 */
    private static final int STATUE_Y = 44;

    /**
     * 四个雕像的正确朝向。
     * 每个元素：{x, z, dx, dz}
     * <ul>
     * <li>雕像1 (-617, 240)：朝北 (z-)，脸朝食物链下一级</li>
     * <li>雕像2 (-607, 240)：朝西 (x-)，指向雕像1</li>
     * <li>雕像3 (-607, 248)：朝北 (z-)，指向雕像2</li>
     * <li>雕像4 (-617, 248)：朝东 (x+)，指向雕像3</li>
     * </ul>
     */
    private static final int[][] STATUES = {
            {-617, 240, 0, -1},
            {-607, 240, -1, 0},
            {-607, 248, 0, -1},
            {-617, 248, 1, 0},
    };

    private TorrhusCanyonTemple() {
    }

    public static void init() {
        // ── Tick：检测谜题进度 ──
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;

            if (!HypixelLocationTracker.getInstance().isIn(MAP_NAME)) return;
            if (!ModConfigManager.get().hunting.torrhusCanyon.templePuzzle) return;

            // 第一关：边缘检测按钮按下事件
            if (!isDoorOpen(client, PUZZLE1_DOOR) && isInPuzzle1Area(client)) {
                updateButtonPress(client);
            } else {
                // 离开区域或门已开 → 重置所有状态
                pressProgress = 0;
                prevPoweredMask = 0;
                areaInitialized = false;
            }
        });

        // ── 渲染：按钮高亮 + 雕像方向线 ──
        RenderPhaseRegister.register(ctx -> {
            var client = Minecraft.getInstance();
            if (client.player == null || client.level == null) return;

            if (!HypixelLocationTracker.getInstance().isIn(MAP_NAME)) return;
            if (!ModConfigManager.get().hunting.torrhusCanyon.templePuzzle) return;

            // 第一关：给当前要按的按钮套半透明盒子
            if (pressProgress < PRESS_ORDER.length
                    && !isDoorOpen(client, PUZZLE1_DOOR)
                    && isInPuzzle1Area(client)) {
                var target = BUTTONS[PRESS_ORDER[pressProgress]];
                drawButtonBox(ctx, target);
            }

            // 第二关：给每个雕像画方向线
            if (!isDoorOpen(client, PUZZLE2_DOOR) && isInPuzzle2Area(client)) {
                drawStatueLines(ctx);
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // 门状态
    // ═══════════════════════════════════════════════════════════════

    private static boolean isDoorOpen(Minecraft client, BlockPos pos) {
        var level = client.level;
        return level == null || level.getBlockState(pos).isAir();
    }

    // ═══════════════════════════════════════════════════════════════
    // 区域检测
    // ═══════════════════════════════════════════════════════════════

    /** 第一关区域：-622 39 218 到 -579 57 233 */
    private static boolean isInPuzzle1Area(Minecraft client) {
        var p = client.player;
        if (p == null) return false;
        return p.getX() >= -622 && p.getX() <= -579
                && p.getY() >= 39 && p.getY() <= 57
                && p.getZ() >= 218 && p.getZ() <= 233;
    }

    /** 第二关区域：-620 42 236 到 -604 57 252 */
    private static boolean isInPuzzle2Area(Minecraft client) {
        var p = client.player;
        if (p == null) return false;
        return p.getX() >= -620 && p.getX() <= -604
                && p.getY() >= 42 && p.getY() <= 57
                && p.getZ() >= 236 && p.getZ() <= 252;
    }

    // ═══════════════════════════════════════════════════════════════
    // 按钮按下边缘检测
    // ═══════════════════════════════════════════════════════════════

    /**
     * 黑石按钮按了后会弹起来（POWERED 只持续约 1 秒），可以同时有多个按钮
     * 处于按下状态，服务器只关心按下的顺序。
     * <p>
     * 用 bitmask 差集检测"新增的按下"（上升沿）：
     * {@code newlyPressed = currentMask & ~prevPoweredMask}
     * <p>
     * 首次进入区域时只记录初始状态不处理，避免把已有的按下误认为新按下。
     */
    private static void updateButtonPress(Minecraft client) {
        var level = client.level;
        if (level == null) return;

        // 构建当前所有处于 POWERED 状态的按钮 bitmask
        int currentMask = 0;
        for (int i = 0; i < BUTTONS.length; i++) {
            var state = level.getBlockState(BUTTONS[i]);
            if (state.hasProperty(BlockStateProperties.POWERED)
                    && state.getValue(BlockStateProperties.POWERED)) {
                currentMask |= (1 << i);
            }
        }

        if (!areaInitialized) {
            // 首次进入区域：只记录当前状态，不做判断
            prevPoweredMask = currentMask;
            areaInitialized = true;
            return;
        }

        // 找出新增的按下（当前有但上一 tick 没有）
        int newlyPressed = currentMask & ~prevPoweredMask;
        if (newlyPressed != 0) {
            for (int i = 0; i < BUTTONS.length; i++) {
                if ((newlyPressed & (1 << i)) == 0) continue;

                if (pressProgress < PRESS_ORDER.length
                        && i == PRESS_ORDER[pressProgress]) {
                    // 按了正确的按钮 → 进度 +1
                    pressProgress++;
                } else {
                    // 按了不该按的按钮 → 进度归零
                    pressProgress = 0;
                    break;
                }
            }
        }

        prevPoweredMask = currentMask;
    }

    // ═══════════════════════════════════════════════════════════════
    // 渲染
    // ═══════════════════════════════════════════════════════════════

    /** 用按钮的真实 VoxelShape 画填充+线框，类似 F3 的 debug hit result */
    private static void drawButtonBox(WorldRenderContext ctx, BlockPos pos) {
        var client = Minecraft.getInstance();
        var level = client.level;
        if (level == null) return;

        var state = level.getBlockState(pos);
        var shape = state.getShape(level, pos);
        if (shape.isEmpty()) return;

        double x1 = pos.getX() + shape.min(Direction.Axis.X) - 0.01;
        double y1 = pos.getY() + shape.min(Direction.Axis.Y) - 0.01;
        double z1 = pos.getZ() + shape.min(Direction.Axis.Z) - 0.01;
        double x2 = pos.getX() + shape.max(Direction.Axis.X) + 0.01;
        double y2 = pos.getY() + shape.max(Direction.Axis.Y) + 0.01;
        double z2 = pos.getZ() + shape.max(Direction.Axis.Z) + 0.01;

        WorldRenderUtils.drawFilledBox(ctx, x1, y1, z1, x2, y2, z2,
                0f, 1f, 0f, 0.3f, true);
        WorldRenderUtils.drawWireframeBox(ctx, x1, y1, z1, x2, y2, z2,
                0f, 1f, 0f, 0.6f, true, 1.0f);
    }

    /** 在雕像位置绘制正确朝向的方向线（细长方体，开深度测试） */
    private static void drawStatueLines(WorldRenderContext ctx) {
        float halfW = 0.04f;

        for (int[] s : STATUES) {
            double cx = s[0] + 0.5;
            double cy = STATUE_Y + 0.5;
            double cz = s[1] + 0.5;
            double ex = cx + s[2] * 1.5;
            double ez = cz + s[3] * 1.5;

            double bx1 = Math.min(cx, ex) - halfW;
            double by1 = cy - halfW;
            double bz1 = Math.min(cz, ez) - halfW;
            double bx2 = Math.max(cx, ex) + halfW;
            double by2 = cy + halfW;
            double bz2 = Math.max(cz, ez) + halfW;

            WorldRenderUtils.drawFilledBox(ctx,
                    bx1, by1, bz1, bx2, by2, bz2,
                    0f, 1f, 1f, 1f, true);
        }
    }
}
