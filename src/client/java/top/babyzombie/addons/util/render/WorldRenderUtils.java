package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public final class WorldRenderUtils {
    private static final RenderPipeline FILLED_N = RenderPipelines.DEBUG_FILLED_BOX;
    private static final RenderPipeline FILLED_X = BzaRenderer.register("filled_x", RenderPipelines.DEBUG_FILLED_SNIPPET, true);
    private static final RenderPipeline LINES_X = RenderPipelines.register(
        com.mojang.blaze3d.pipeline.RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("babyzombieaddons", "pipeline/lines_x"))
            .withDepthStencilState(java.util.Optional.empty())
            .build());

    public static void drawFilledBox(WorldRenderContext ctx, double x1,double y1,double z1,double x2,double y2,double z2, float r,float g,float b,float a, boolean through) {
        drawFilledBox(ctx.worldState().cameraRenderState, x1,y1,z1,x2,y2,z2,r,g,b,a,through);
    }
    public static void drawFilledBox(net.minecraft.client.renderer.state.level.CameraRenderState cam, double x1,double y1,double z1,double x2,double y2,double z2, float r,float g,float b,float a, boolean through) {
        var m = BzaRenderer.cameraMatrix(cam);
        var buf = BzaRenderer.begin(through ? FILLED_X : FILLED_N, PrimitiveTopology.QUADS);
        float X1=(float)x1,Y1=(float)y1,Z1=(float)z1,X2=(float)x2,Y2=(float)y2,Z2=(float)z2;
        q(buf,m,X1,Y1,Z1,r,g,b,a);q(buf,m,X2,Y1,Z1,r,g,b,a);q(buf,m,X2,Y1,Z2,r,g,b,a);q(buf,m,X1,Y1,Z2,r,g,b,a);
        q(buf,m,X1,Y2,Z2,r,g,b,a);q(buf,m,X2,Y2,Z2,r,g,b,a);q(buf,m,X2,Y2,Z1,r,g,b,a);q(buf,m,X1,Y2,Z1,r,g,b,a);
        q(buf,m,X1,Y1,Z2,r,g,b,a);q(buf,m,X2,Y1,Z2,r,g,b,a);q(buf,m,X2,Y2,Z2,r,g,b,a);q(buf,m,X1,Y2,Z2,r,g,b,a);
        q(buf,m,X2,Y1,Z1,r,g,b,a);q(buf,m,X1,Y1,Z1,r,g,b,a);q(buf,m,X1,Y2,Z1,r,g,b,a);q(buf,m,X2,Y2,Z1,r,g,b,a);
        q(buf,m,X2,Y1,Z2,r,g,b,a);q(buf,m,X2,Y1,Z1,r,g,b,a);q(buf,m,X2,Y2,Z1,r,g,b,a);q(buf,m,X2,Y2,Z2,r,g,b,a);
        q(buf,m,X1,Y1,Z1,r,g,b,a);q(buf,m,X1,Y1,Z2,r,g,b,a);q(buf,m,X1,Y2,Z2,r,g,b,a);q(buf,m,X1,Y2,Z1,r,g,b,a);
    }

    public static void drawWireframeBox(WorldRenderContext ctx, double x1,double y1,double z1,double x2,double y2,double z2, float r,float g,float b,float a, boolean through, float lw) {
        drawWireframeBox(ctx.worldState().cameraRenderState, x1,y1,z1,x2,y2,z2,r,g,b,a,through);
    }
    public static void drawWireframeBox(net.minecraft.client.renderer.state.level.CameraRenderState cam, double x1,double y1,double z1,double x2,double y2,double z2, float r,float g,float b,float a, boolean through) {
        var m = BzaRenderer.cameraMatrix(cam);
        var buf = BzaRenderer.begin(through ? LINES_X : RenderPipelines.LINES, PrimitiveTopology.LINES);
        float X1=(float)x1,Y1=(float)y1,Z1=(float)z1,X2=(float)x2,Y2=(float)y2,Z2=(float)z2;
        l(buf,m,X1,Y1,Z1,r,g,b,a);l(buf,m,X2,Y1,Z1,r,g,b,a);l(buf,m,X2,Y1,Z1,r,g,b,a);l(buf,m,X2,Y1,Z2,r,g,b,a);
        l(buf,m,X2,Y1,Z2,r,g,b,a);l(buf,m,X1,Y1,Z2,r,g,b,a);l(buf,m,X1,Y1,Z2,r,g,b,a);l(buf,m,X1,Y1,Z1,r,g,b,a);
        l(buf,m,X1,Y2,Z1,r,g,b,a);l(buf,m,X2,Y2,Z1,r,g,b,a);l(buf,m,X2,Y2,Z1,r,g,b,a);l(buf,m,X2,Y2,Z2,r,g,b,a);
        l(buf,m,X2,Y2,Z2,r,g,b,a);l(buf,m,X1,Y2,Z2,r,g,b,a);l(buf,m,X1,Y2,Z2,r,g,b,a);l(buf,m,X1,Y2,Z1,r,g,b,a);
        l(buf,m,X1,Y1,Z1,r,g,b,a);l(buf,m,X1,Y2,Z1,r,g,b,a);l(buf,m,X2,Y1,Z1,r,g,b,a);l(buf,m,X2,Y2,Z1,r,g,b,a);
        l(buf,m,X2,Y1,Z2,r,g,b,a);l(buf,m,X2,Y2,Z2,r,g,b,a);l(buf,m,X1,Y1,Z2,r,g,b,a);l(buf,m,X1,Y2,Z2,r,g,b,a);
    }

    public static void drawLine(WorldRenderContext ctx, double x1,double y1,double z1,double x2,double y2,double z2, float r,float g,float b,float a, boolean through, float lw) {
        drawLine(ctx.worldState().cameraRenderState, x1,y1,z1,x2,y2,z2,r,g,b,a,through);
    }
    public static void drawLine(net.minecraft.client.renderer.state.level.CameraRenderState cam, double x1,double y1,double z1,double x2,double y2,double z2, float r,float g,float b,float a, boolean through) {
        var m = BzaRenderer.cameraMatrix(cam);
        var buf = BzaRenderer.begin(through ? LINES_X : RenderPipelines.LINES, PrimitiveTopology.LINES);
        buf.addVertex(m,(float)x1,(float)y1,(float)z1).setColor(r,g,b,a);
        buf.addVertex(m,(float)x2,(float)y2,(float)z2).setColor(r,g,b,a);
    }

    public static void close() { BzaRenderer.close(); }

    private static void q(VertexConsumer b, Matrix4f m, float x,float y,float z, float r,float g,float bl,float a){ b.addVertex(m,x,y,z).setColor(r,g,bl,a); }
    private static void l(VertexConsumer b, Matrix4f m, float x,float y,float z, float r,float g,float bl,float a){ b.addVertex(m,x,y,z).setColor(r,g,bl,a).setNormal(0,1,0).setLineWidth(2f); }
}
