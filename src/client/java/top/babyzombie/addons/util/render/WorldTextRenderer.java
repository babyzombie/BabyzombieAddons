package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Renders floating text in the world at the given coordinates. */
public final class WorldTextRenderer {

    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(256);

    private WorldTextRenderer() {}

    /** Render a string at world coordinates (x,y,z) with the given ARGB color and scale. */
    public static void renderString(WorldRenderContext context, String text, double x, double y, double z,
                                     int color, float scale, boolean throughWalls) {
        renderString(context, text, x, y, z, color, scale, throughWalls, 0);
    }

    /** Render a string at world coordinates (x,y,z) with a screen-space vertical offset. */
    public static void renderString(WorldRenderContext context, String text, double x, double y, double z,
                                     int color, float scale, boolean throughWalls, float fontYOffset) {
        var font = Minecraft.getInstance().font;
        var matrices = context.matrices();
        Vec3 camera = context.worldState().cameraRenderState.pos;
        var bufferSource = MultiBufferSource.immediate(ALLOCATOR);
        var displayMode = throughWalls ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        matrices.translate(x, y, z);
        matrices.mulPose(context.worldState().cameraRenderState.orientation);
        matrices.scale(scale, -scale, scale);

        font.drawInBatch(
            Component.literal(text).getVisualOrderText(),
            -font.width(text) / 2f, fontYOffset,
            color, false,
            new Matrix4f(matrices.last().pose()),
            bufferSource,
            displayMode,
            0xF000F0,
            0
        );

        matrices.popPose();
        bufferSource.endBatch();
    }
}
