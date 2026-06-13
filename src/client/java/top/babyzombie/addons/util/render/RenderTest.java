package top.babyzombie.addons.util.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;

public final class RenderTest {

    private RenderTest() {}

    public static void init() {
        LevelRenderEvents.END_MAIN.register(ctx -> {
            var p = Minecraft.getInstance().player;
            if (p == null) return;
            var cam = ctx.levelState().cameraRenderState;

            // Space out the test shapes
            double bx = 0, bz = 0, by = -50;

            // Red filled box (left)
            WorldRenderUtils.drawFilledBox(cam, bx - 5, by, bz, bx - 2, by + 3, bz + 3, 1, 0, 0, 0.4f, false);
            // Green wireframe (right)
            WorldRenderUtils.drawWireframeBox(cam, bx + 2, by, bz, bx + 5, by + 3, bz + 3, 0, 1, 0, 0.8f, true);
            // Blue beacon (center)
            BeamRenderer.drawBeam(cam, bx, by, bz, 10, 0.5f, 0xFFFFFFFF);
            // White text (above)
            WorldTextRenderer.renderString(cam, "Box", bx - 3.5f, by + 3.5f, bz + 1.5f, 0xFFFFFFFF, 0.05f, true);
            WorldTextRenderer.renderString(cam, "Wire", bx + 3.5f, by + 3.5f, bz + 1.5f, 0xFF00FF00, 0.05f, true);
            WorldTextRenderer.renderString(cam, "Beam", bx, by + 10.5f, bz + 1.5f, 0xFF4488FF, 0.05f, true);
        });
    }
}
