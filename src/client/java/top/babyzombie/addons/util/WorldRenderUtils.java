package top.babyzombie.addons.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Utility for rendering wireframe shapes (boxes, lines) in the 3D world.
 * Call from a {@code WorldRenderEvents} handler.
 */
public final class WorldRenderUtils {

    private static final RenderType LINES = createType(false);
    private static final RenderType LINES_XRAY = createType(true);

    private WorldRenderUtils() {}

    private static RenderType createType(boolean xray) {
        RenderPipeline.Builder b = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.LINES)
            .withLocation(xray ? "bza_lines_xray" : "bza_lines");
        if (xray) b.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST);
        var snippet = b.buildSnippet();
        var pipeline = RenderPipeline.builder(snippet)
            .withLocation(xray ? "bza_lines_xray" : "bza_lines")
            .build();
        return RenderType.create(xray ? "bza_lines_xray" : "bza_lines",
            RenderSetup.builder(pipeline).bufferSize(1536).createRenderSetup());
    }

    public static void drawBox(double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ,
                                float r, float g, float b, float a) {
        drawBox(minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a, false);
    }

    public static void drawBoxAtEntity(double entityX, double entityY, double entityZ,
                                        double boxW, double boxH, double boxD,
                                        float r, float g, float b, float a) {
        drawBox(entityX - boxW / 2.0, entityY, entityZ - boxD / 2.0,
                entityX + boxW / 2.0, entityY + boxH, entityZ + boxD / 2.0, r, g, b, a, false);
    }

    public static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2,
                                 float r, float g, float b, float a) {
        drawLine(x1, y1, z1, x2, y2, z2, r, g, b, a, false);
    }

    public static void drawBoxXray(double minX, double minY, double minZ,
                                    double maxX, double maxY, double maxZ,
                                    float r, float g, float b, float a) {
        drawBox(minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a, true);
    }

    public static void drawBoxAtEntityXray(double entityX, double entityY, double entityZ,
                                            double boxW, double boxH, double boxD,
                                            float r, float g, float b, float a) {
        drawBox(entityX - boxW / 2.0, entityY, entityZ - boxD / 2.0,
                entityX + boxW / 2.0, entityY + boxH, entityZ + boxD / 2.0, r, g, b, a, true);
    }

    public static void drawLineXray(double x1, double y1, double z1, double x2, double y2, double z2,
                                     float r, float g, float b, float a) {
        drawLine(x1, y1, z1, x2, y2, z2, r, g, b, a, true);
    }

    private static void drawBox(double minX, double minY, double minZ,
                                 double maxX, double maxY, double maxZ,
                                 float r, float g, float b, float a, boolean xray) {
        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        float cx = (float) cam.position().x, cy = (float) cam.position().y, cz = (float) cam.position().z;
        float x1 = (float) minX - cx, y1 = (float) minY - cy, z1 = (float) minZ - cz;
        float x2 = (float) maxX - cx, y2 = (float) maxY - cy, z2 = (float) maxZ - cz;
        int cr = c(r), cg = c(g), cb = c(b), ca = c(a);

        var tess = Tesselator.getInstance();
        var builder = tess.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
        L(builder, x1, y1, z1, x2, y1, z1, cr, cg, cb, ca);
        L(builder, x2, y1, z1, x2, y1, z2, cr, cg, cb, ca);
        L(builder, x2, y1, z2, x1, y1, z2, cr, cg, cb, ca);
        L(builder, x1, y1, z2, x1, y1, z1, cr, cg, cb, ca);
        L(builder, x1, y2, z1, x2, y2, z1, cr, cg, cb, ca);
        L(builder, x2, y2, z1, x2, y2, z2, cr, cg, cb, ca);
        L(builder, x2, y2, z2, x1, y2, z2, cr, cg, cb, ca);
        L(builder, x1, y2, z2, x1, y2, z1, cr, cg, cb, ca);
        L(builder, x1, y1, z1, x1, y2, z1, cr, cg, cb, ca);
        L(builder, x2, y1, z1, x2, y2, z1, cr, cg, cb, ca);
        L(builder, x2, y1, z2, x2, y2, z2, cr, cg, cb, ca);
        L(builder, x1, y1, z2, x1, y2, z2, cr, cg, cb, ca);
        (xray ? LINES_XRAY : LINES).draw(builder.build());
    }

    private static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2,
                                  float r, float g, float b, float a, boolean xray) {
        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        float cx = (float) cam.position().x, cy = (float) cam.position().y, cz = (float) cam.position().z;
        int cr = c(r), cg = c(g), cb = c(b), ca = c(a);
        var tess = Tesselator.getInstance();
        var builder = tess.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
        L(builder, (float)x1 - cx, (float)y1 - cy, (float)z1 - cz,
          (float)x2 - cx, (float)y2 - cy, (float)z2 - cz, cr, cg, cb, ca);
        (xray ? LINES_XRAY : LINES).draw(builder.build());
    }

    private static void L(BufferBuilder b, float x1, float y1, float z1,
                           float x2, float y2, float z2, int r, int g, int bl, int a) {
        b.addVertex(x1, y1, z1).setColor(r, g, bl, a);
        b.addVertex(x2, y2, z2).setColor(r, g, bl, a);
    }

    private static int c(float v) { return (int)(v * 255); }
}
