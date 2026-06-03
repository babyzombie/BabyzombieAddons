package top.babyzombie.addons.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Utility for rendering wireframe shapes (boxes, lines) in the 3D world.
 * Call from a {@code WorldRenderEvents} handler.
 */
public final class WorldRenderUtils {

    private static final RenderType LINES = createType(RenderPipelines.LINES);
    private static final RenderType LINES_XRAY = createXrayType();

    private WorldRenderUtils() {}

    private static RenderType createType(com.mojang.blaze3d.pipeline.RenderPipeline pipeline) {
        return RenderType.create("bza_lines",
            RenderSetup.builder(pipeline).bufferSize(1536).createRenderSetup());
    }

    private static RenderType createXrayType() {
        var snippet = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .buildSnippet();
        var pipeline = RenderPipeline.builder(snippet).build();
        return RenderType.create("bza_lines_xray",
            RenderSetup.builder(pipeline).bufferSize(1536).createRenderSetup());
    }

    // ---- Normal (respects depth) ----

    public static void drawBox(double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ,
                                float r, float g, float b, float a) {
        drawBox(minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a, false);
    }

    public static void drawBoxAtEntity(double entityX, double entityY, double entityZ,
                                        double boxW, double boxH, double boxD,
                                        float r, float g, float b, float a) {
        drawBoxAtEntity(entityX, entityY, entityZ, boxW, boxH, boxD, r, g, b, a, false);
    }

    public static void drawLine(double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 float r, float g, float b, float a) {
        drawLine(x1, y1, z1, x2, y2, z2, r, g, b, a, false);
    }

    // ---- X-ray (ignores depth) ----

    public static void drawBoxXray(double minX, double minY, double minZ,
                                    double maxX, double maxY, double maxZ,
                                    float r, float g, float b, float a) {
        drawBox(minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a, true);
    }

    public static void drawBoxAtEntityXray(double entityX, double entityY, double entityZ,
                                            double boxW, double boxH, double boxD,
                                            float r, float g, float b, float a) {
        drawBoxAtEntity(entityX, entityY, entityZ, boxW, boxH, boxD, r, g, b, a, true);
    }

    public static void drawLineXray(double x1, double y1, double z1,
                                     double x2, double y2, double z2,
                                     float r, float g, float b, float a) {
        drawLine(x1, y1, z1, x2, y2, z2, r, g, b, a, true);
    }

    // ---- Internal ----

    public static void drawBox(double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ,
                                float r, float g, float b, float a, boolean xray) {
        float x1 = (float) minX, y1 = (float) minY, z1 = (float) minZ;
        float x2 = (float) maxX, y2 = (float) maxY, z2 = (float) maxZ;
        int cr = c(r), cg = c(g), cb = c(b), ca = c(a);

        var tess = Tesselator.getInstance();
        var builder = tess.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
        line(builder, x1, y1, z1, x2, y1, z1, cr, cg, cb, ca);
        line(builder, x2, y1, z1, x2, y1, z2, cr, cg, cb, ca);
        line(builder, x2, y1, z2, x1, y1, z2, cr, cg, cb, ca);
        line(builder, x1, y1, z2, x1, y1, z1, cr, cg, cb, ca);
        line(builder, x1, y2, z1, x2, y2, z1, cr, cg, cb, ca);
        line(builder, x2, y2, z1, x2, y2, z2, cr, cg, cb, ca);
        line(builder, x2, y2, z2, x1, y2, z2, cr, cg, cb, ca);
        line(builder, x1, y2, z2, x1, y2, z1, cr, cg, cb, ca);
        line(builder, x1, y1, z1, x1, y2, z1, cr, cg, cb, ca);
        line(builder, x2, y1, z1, x2, y2, z1, cr, cg, cb, ca);
        line(builder, x2, y1, z2, x2, y2, z2, cr, cg, cb, ca);
        line(builder, x1, y1, z2, x1, y2, z2, cr, cg, cb, ca);

        (xray ? LINES_XRAY : LINES).draw(builder.build());
    }

    public static void drawBoxAtEntity(double entityX, double entityY, double entityZ,
                                        double boxW, double boxH, double boxD,
                                        float r, float g, float b, float a, boolean xray) {
        drawBox(entityX - boxW / 2.0, entityY, entityZ - boxD / 2.0,
                entityX + boxW / 2.0, entityY + boxH, entityZ + boxD / 2.0,
                r, g, b, a, xray);
    }

    public static void drawLine(double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 float r, float g, float b, float a, boolean xray) {
        int cr = c(r), cg = c(g), cb = c(b), ca = c(a);
        var tess = Tesselator.getInstance();
        var builder = tess.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
        line(builder, (float) x1, (float) y1, (float) z1,
            (float) x2, (float) y2, (float) z2, cr, cg, cb, ca);
        (xray ? LINES_XRAY : LINES).draw(builder.build());
    }

    private static void line(BufferBuilder builder, float x1, float y1, float z1,
                              float x2, float y2, float z2, int r, int g, int b, int a) {
        builder.addVertex(x1, y1, z1).setColor(r, g, b, a);
        builder.addVertex(x2, y2, z2).setColor(r, g, b, a);
    }

    private static int c(float v) { return (int)(v * 255); }
}
