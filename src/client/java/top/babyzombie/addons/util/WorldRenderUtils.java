package top.babyzombie.addons.util;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class WorldRenderUtils {

    static final RenderType BOX, BOX_XRAY;
    private static final float THICK_SCALE = 0.004f; // converts lineWidth pixels to world units
    static {
        var snippet = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withLocation("bza_box")
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .buildSnippet();
        var pipeline = RenderPipeline.builder(snippet).withLocation("bza_box").build();
        BOX = RenderType.create("bza_box", RenderSetup.builder(pipeline).bufferSize(1536).createRenderSetup());

        var snippetX = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withLocation("bza_box_xray")
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .buildSnippet();
        var pipelineX = RenderPipeline.builder(snippetX).withLocation("bza_box_xray").build();
        BOX_XRAY = RenderType.create("bza_box_xray", RenderSetup.builder(pipelineX).bufferSize(1536).createRenderSetup());
    }

    private WorldRenderUtils() {}

    // ---- public API ----
    public static void drawBox(double x1, double y1, double z1, double x2, double y2, double z2,
                                float r, float g, float b, float a)       { drawBox(x1,y1,z1,x2,y2,z2,r,g,b,a,false,3); }
    public static void drawBox(double x1, double y1, double z1, double x2, double y2, double z2,
                                float r, float g, float b, float a, float lw) { drawBox(x1,y1,z1,x2,y2,z2,r,g,b,a,false,lw); }
    public static void drawBoxXray(double x1, double y1, double z1, double x2, double y2, double z2,
                                    float r, float g, float b, float a)    { drawBox(x1,y1,z1,x2,y2,z2,r,g,b,a,true,3); }
    public static void drawBoxXray(double x1, double y1, double z1, double x2, double y2, double z2,
                                    float r, float g, float b, float a, float lw) { drawBox(x1,y1,z1,x2,y2,z2,r,g,b,a,true,lw); }

    public static void drawBoxAtEntity(double ex, double ey, double ez, double w, double h, double d,
                                        float r, float g, float b, float a) {
        drawBox(ex-w/2, ey, ez-d/2, ex+w/2, ey+h, ez+d/2, r,g,b,a, false, 3);
    }
    public static void drawBoxAtEntityXray(double ex, double ey, double ez, double w, double h, double d,
                                            float r, float g, float b, float a) {
        drawBox(ex-w/2, ey, ez-d/2, ex+w/2, ey+h, ez+d/2, r,g,b,a, true, 3);
    }

    public static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2,
                                 float r, float g, float b, float a)       { drawLine(x1,y1,z1,x2,y2,z2,r,g,b,a,false,3); }
    public static void drawLineXray(double x1, double y1, double z1, double x2, double y2, double z2,
                                     float r, float g, float b, float a)    { drawLine(x1,y1,z1,x2,y2,z2,r,g,b,a,true,3); }

    // ---- private ----
    private static void drawBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                                 float r, float g, float b, float a, boolean xray, float lw) {
        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        float cx = (float) cam.position().x, cy = (float) cam.position().y, cz = (float) cam.position().z;
        float x1 = (float) minX - cx, y1 = (float) minY - cy, z1 = (float) minZ - cz;
        float x2 = (float) maxX - cx, y2 = (float) maxY - cy, z2 = (float) maxZ - cz;
        int cr = (int)(r*255), cg = (int)(g*255), cb = (int)(b*255), ca = (int)(a*255);
        float t = lw * THICK_SCALE;

        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        thickEdge(buf, x1,y1,z1, x2,y1,z1, t, cr,cg,cb,ca);
        thickEdge(buf, x2,y1,z1, x2,y1,z2, t, cr,cg,cb,ca);
        thickEdge(buf, x2,y1,z2, x1,y1,z2, t, cr,cg,cb,ca);
        thickEdge(buf, x1,y1,z2, x1,y1,z1, t, cr,cg,cb,ca);
        thickEdge(buf, x1,y2,z1, x2,y2,z1, t, cr,cg,cb,ca);
        thickEdge(buf, x2,y2,z1, x2,y2,z2, t, cr,cg,cb,ca);
        thickEdge(buf, x2,y2,z2, x1,y2,z2, t, cr,cg,cb,ca);
        thickEdge(buf, x1,y2,z2, x1,y2,z1, t, cr,cg,cb,ca);
        thickEdge(buf, x1,y1,z1, x1,y2,z1, t, cr,cg,cb,ca);
        thickEdge(buf, x2,y1,z1, x2,y2,z1, t, cr,cg,cb,ca);
        thickEdge(buf, x2,y1,z2, x2,y2,z2, t, cr,cg,cb,ca);
        thickEdge(buf, x1,y1,z2, x1,y2,z2, t, cr,cg,cb,ca);
        var mesh = buf.build();
        if (mesh != null) (xray ? BOX_XRAY : BOX).draw(mesh);
    }

    private static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2,
                                  float r, float g, float b, float a, boolean xray, float lw) {
        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        float cx = (float) cam.position().x, cy = (float) cam.position().y, cz = (float) cam.position().z;
        int cr = (int)(r*255), cg = (int)(g*255), cb = (int)(b*255), ca = (int)(a*255);
        float t = lw * THICK_SCALE;
        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        thickEdge(buf, (float)x1-cx, (float)y1-cy, (float)z1-cz,
                  (float)x2-cx, (float)y2-cy, (float)z2-cz, t, cr,cg,cb,ca);
        var mesh = buf.build();
        if (mesh != null) (xray ? BOX_XRAY : BOX).draw(mesh);
    }

    /** Draw a thick line segment as a quad with consistent screen-space thickness */
    private static void thickEdge(VertexConsumer b, float x1, float y1, float z1,
                                   float x2, float y2, float z2, float t,
                                   int r, int g, int bl, int a) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 0.0001f) return;
        // Pick a perpendicular direction that's orthogonal to the edge AND roughly in screen plane
        float px, py, pz;
        if (Math.abs(dy) < 0.999f * len) {
            // Cross with world-up (0,1,0): gives horizontal perpendicular
            px = -dz; py = 0; pz = dx;
        } else {
            // Nearly vertical: cross with forward (0,0,1)
            px = 1; py = 0; pz = 0;
        }
        float plen = (float)Math.sqrt(px*px + py*py + pz*pz);
        px = px / plen * t; py = py / plen * t; pz = pz / plen * t;
        b.addVertex(x1 + px, y1 + py, z1 + pz).setColor(r, g, bl, a);
        b.addVertex(x1 - px, y1 - py, z1 - pz).setColor(r, g, bl, a);
        b.addVertex(x2 - px, y2 - py, z2 - pz).setColor(r, g, bl, a);
        b.addVertex(x2 + px, y2 + py, z2 + pz).setColor(r, g, bl, a);
    }
}
