package top.babyzombie.addons.module.mining;

import top.babyzombie.addons.util.render.RenderPhaseRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.render.WorldRenderUtils;
import top.babyzombie.addons.util.render.WorldTextRenderer;

public final class GreatGlaciteWaypoints {
    private static final double[][][] POSITIONS = {
            {{52, 154, 274}, {57.25, 154.8, 274.25}},
            {{18, 164, 303}, {22.5, 164.6, 302.5}},
            {{99, 135, 318}, {103.25, 135.6, 318.25}},
            {{110, 139, 337}, {110.25, 140.25, 332.6}},
            {{-32, 130, 332}, {-31.75, 128.6, 332.25}},
    };

    private GreatGlaciteWaypoints() {}

    public static void init() {
        RenderPhaseRegister.register(ctx -> {
            if (!ModConfigManager.get().mining.greatGlaciteWaypoints) return;
            if (!isInGlaciteArea()) return;

            var player = Minecraft.getInstance().player;
            for (var p : POSITIONS) {
                double x = p[0][0], y = p[0][1], z = p[0][2];
                WorldRenderUtils.drawFilledBox(ctx,
                    x, y, z,
                    x + 1, y + 1, z + 1,
                    0, 1, 1, 0.3f, true);
                WorldTextRenderer.renderString(ctx, "§bGreat Glacite", x + 0.5, y, z + 0.5, 0xFF00FFFF, 0.04f, true);

                if (player != null && player.position().distanceTo(new Vec3(x, y, z)) < 5) {
                    double x2 = p[1][0], y2 = p[1][1], z2 = p[1][2];
                    WorldRenderUtils.drawFilledBox(ctx,
                        x2, y2, z2,
                        x2 + 0.5, y2 + 0.5, z2 + 0.5,
                        0.2f, 0.2f, 1, 1, false);
                }
            }
        });
    }

    private static boolean isInGlaciteArea() {
        var t = HypixelLocationTracker.getInstance();
        if (!t.isIn("Dwarven Mines")) return false;
        String loc = t.getLocation();
        return "Dwarven Base Camp".equals(loc) || "Glacite Tunnels".equals(loc) || "Great Glacite Lake".equals(loc);
    }
}
