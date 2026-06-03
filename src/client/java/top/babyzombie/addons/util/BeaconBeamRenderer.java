package top.babyzombie.addons.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;

import java.awt.Color;

/**
 * Stateless utility for rendering solid-color beacon beams.
 * Call directly from a {@code WorldRenderEvents} handler.
 */
public final class BeaconBeamRenderer {

    public static final float DEFAULT_HEIGHT = 300f;
    private static final RenderType BEAM;
    static {
        var snippet = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withLocation("bza_beam")
            .withCull(false)
            .withBlend(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.LEQUAL_DEPTH_TEST)
            .buildSnippet();
        var pipeline = RenderPipeline.builder(snippet).withLocation("bza_beam").build();
        var setup = RenderSetup.builder(pipeline).bufferSize(1536).createRenderSetup();
        BEAM = RenderType.create("bza_beam", setup);
    }

    private BeaconBeamRenderer() {}

    public static void render(double x, double y, double z, Color color, float h) {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        var cam = client.gameRenderer.getMainCamera();
        float cx = (float) cam.position().x, cy = (float) cam.position().y, cz = (float) cam.position().z;
        float r = color.getRed() / 255f, g = color.getGreen() / 255f, b = color.getBlue() / 255f, a = color.getAlpha() / 255f;
        float bx = (float) x - cx, by = (float) y - cy, bz = (float) z - cz;

        float time = client.level.getGameTime() + client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        double d2 = time * 0.05 * -2.5;
        double d4=0.5+Math.cos(d2+2.356)*0.2, d5=0.5+Math.sin(d2+2.356)*0.2;
        double d6=0.5+Math.cos(d2+Math.PI/4)*0.2, d7=0.5+Math.sin(d2+Math.PI/4)*0.2;
        double d8=0.5+Math.cos(d2+3.927)*0.2, d9=0.5+Math.sin(d2+3.927)*0.2;
        double d10=0.5+Math.cos(d2+5.498)*0.2, d11=0.5+Math.sin(d2+5.498)*0.2;

        var tess = Tesselator.getInstance();
        var buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        v(buf,bx+d4,by+h,bz+d5,r,g,b,a); v(buf,bx+d4,by,bz+d5,r,g,b,1);
        v(buf,bx+d6,by,bz+d7,r,g,b,1); v(buf,bx+d6,by+h,bz+d7,r,g,b,a);
        v(buf,bx+d10,by+h,bz+d11,r,g,b,a); v(buf,bx+d10,by,bz+d11,r,g,b,1);
        v(buf,bx+d8,by,bz+d9,r,g,b,1); v(buf,bx+d8,by+h,bz+d9,r,g,b,a);
        v(buf,bx+d6,by+h,bz+d7,r,g,b,a); v(buf,bx+d6,by,bz+d7,r,g,b,1);
        v(buf,bx+d10,by,bz+d11,r,g,b,1); v(buf,bx+d10,by+h,bz+d11,r,g,b,a);
        v(buf,bx+d8,by+h,bz+d9,r,g,b,a); v(buf,bx+d8,by,bz+d9,r,g,b,1);
        v(buf,bx+d4,by,bz+d5,r,g,b,1); v(buf,bx+d4,by+h,bz+d5,r,g,b,a);
        BEAM.draw(buf.build());

        var buf2 = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float ia = a * 0.25f;
        v(buf2,bx+0.2f,by+h,bz+0.2f,r,g,b,ia); v(buf2,bx+0.2f,by,bz+0.2f,r,g,b,0.25f);
        v(buf2,bx+0.8f,by,bz+0.2f,r,g,b,0.25f); v(buf2,bx+0.8f,by+h,bz+0.2f,r,g,b,ia);
        v(buf2,bx+0.8f,by+h,bz+0.8f,r,g,b,ia); v(buf2,bx+0.8f,by,bz+0.8f,r,g,b,0.25f);
        v(buf2,bx+0.2f,by,bz+0.8f,r,g,b,0.25f); v(buf2,bx+0.2f,by+h,bz+0.8f,r,g,b,ia);
        v(buf2,bx+0.8f,by+h,bz+0.2f,r,g,b,ia); v(buf2,bx+0.8f,by,bz+0.2f,r,g,b,0.25f);
        v(buf2,bx+0.8f,by,bz+0.8f,r,g,b,0.25f); v(buf2,bx+0.8f,by+h,bz+0.8f,r,g,b,ia);
        v(buf2,bx+0.2f,by+h,bz+0.8f,r,g,b,ia); v(buf2,bx+0.2f,by,bz+0.8f,r,g,b,0.25f);
        v(buf2,bx+0.2f,by,bz+0.2f,r,g,b,0.25f); v(buf2,bx+0.2f,by+h,bz+0.2f,r,g,b,ia);
        BEAM.draw(buf2.build());
    }

    private static void v(BufferBuilder b, double x, double y, double z, float r, float g, float bl, float a) {
        b.addVertex((float)x, (float)y, (float)z).setColor(r, g, bl, a);
    }
}
