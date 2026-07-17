package top.babyzombie.addons.module.garden;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.state.level.WorldBorderRenderState;
import net.minecraft.world.phys.Vec3;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * 在花园中渲染当前所在地皮及谷仓区域的世界边界。
 *
 * <p>直接调用原版 {@link WorldBorderRenderer}，使用 forcefield 纹理
 * 在地皮四周渲染带色调的半透明边界墙，与 vanilla 世界边界视觉效果完全一致。
 */
public final class PlotBorderDisplay {

    private static WorldBorderRenderer borderRenderer;
    private static final WorldBorderRenderState borderState = new WorldBorderRenderState();

    private PlotBorderDisplay() {}

    public static void init() {
        RenderPhaseRegister.register(ctx -> {
            var config = ModConfigManager.get().garden.plotBorder;
            if (!config.enabled) return;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isIn("Garden")) return;

            Plot plot = PlotUtils.getCurrentPlot();
            if (plot == null) return;

            if (borderRenderer == null) {
                borderRenderer = new WorldBorderRenderer();
            }

            int tint = config.color.getEffectiveColourRGB();
            Vec3 camera = ctx.worldState().cameraRenderState.pos;
            double renderDist = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0;

            borderRenderer.invalidate();
            borderState.minX = plot.minX();
            borderState.maxX = plot.maxX() + 1;
            borderState.minZ = plot.minZ();
            borderState.maxZ = plot.maxZ() + 1;
            borderState.alpha = 1.0;
            borderState.tint = tint;

            borderRenderer.render(borderState, camera, renderDist, 256.0);
        });
    }
}
