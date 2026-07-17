package top.babyzombie.addons.module.hunting.safari;

import net.minecraft.core.BlockPos;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.render.BeamRenderer;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * 在 Safari 区域显示七口铃铛的彩虹信标光柱。
 * 坐标硬编码，用户在设置中只需开启开关即可。
 */
public final class SafariBellDisplay {

    // 彩虹七色 ARGB
    private static final int[] BELL_COLORS = {
        0xFFFF0000, // Red
        0xFFFF7F00, // Orange
        0xFFFFFF00, // Yellow
        0xFF00FF00, // Green
        0xFF0000FF, // Blue
        0xFF4B0082, // Indigo
        0xFF8B00FF  // Violet
    };

    // 七口钟坐标
    private static final BlockPos[] BELL_POS = {
        new BlockPos(-50, 81, 0), // Red
        new BlockPos(-4, 96, -42), // Orange
        new BlockPos(-30, 125, 59), // Yellow
        new BlockPos(47, 55, -7), // Green
        new BlockPos(-90, 109, 16), // Blue
        new BlockPos(-96, 46, -57), // Indigo
        new BlockPos(-68, 66, -43), // Violet
    };

    private static final double BEAM_HEIGHT = 256;
    private static final float BEAM_HALF_WIDTH = 0.15f;

    private SafariBellDisplay() {}

    public static void init() {
        RenderPhaseRegister.register(ctx -> {
            if (!ModConfigManager.get().hunting.safari.bellDisplay) return;
            if (!HypixelLocationTracker.getInstance().isInSafari()) return;

            for (int i = 0; i < BELL_POS.length; i++) {
                BlockPos pos = BELL_POS[i];

                BeamRenderer.drawBeam(ctx,
                    pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    BEAM_HEIGHT, BEAM_HALF_WIDTH,
                    BELL_COLORS[i]);
            }
        });
    }
}
