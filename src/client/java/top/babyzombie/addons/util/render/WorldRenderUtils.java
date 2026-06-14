package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/** Draws filled boxes, wireframe boxes, and lines in the world using custom render pipelines. */
public final class WorldRenderUtils {
    private static final RenderPipeline FILLED_N = RenderPipelines.DEBUG_FILLED_BOX;
    private static final RenderPipeline FILLED_X = BzaRenderer.register("filled_x", RenderPipelines.DEBUG_FILLED_SNIPPET, true);
    private static final RenderPipeline LINES_X = RenderPipelines.register(
        com.mojang.blaze3d.pipeline.RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("babyzombieaddons", "pipeline/lines_x"))
            .withDepthStencilState(java.util.Optional.empty())
            .build());

    private static final String MOD_ID = "babyzombieaddons";

    // ── Pipelines ──────────────────────────────────────────────
    private static final RenderPipeline FILLED_DEPTH = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/bza_filled_depth"))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .build()
    );
    private static final RenderPipeline FILLED_NO_DEPTH = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/bza_filled_no_depth"))
            .withDepthStencilState(Optional.empty())
            .build()
    );
    private static final RenderPipeline LINES_DEPTH = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/bza_lines_depth"))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .build()
    );
    private static final RenderPipeline LINES_NO_DEPTH = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/bza_lines_no_depth"))
            .withDepthStencilState(Optional.empty())
            .build()
    );

    // ── Buffer management ──────────────────────────────────────
    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(1536);
    private static MappableRingBuffer filledVertexBuffer;
    private static MappableRingBuffer linesVertexBuffer;
    private static BufferBuilder filledBuf;
    private static BufferBuilder linesBuf;

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private WorldRenderUtils() {}

    // ═══════════════════════════════════════════════════════════════
    // Pose helper
    // ═══════════════════════════════════════════════════════════════

    private static Matrix4fc applyCameraTransform(WorldRenderContext context) {
        PoseStack matrices = context.matrices();
        Vec3 camera = context.worldState().cameraRenderState.pos;
        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        return matrices.last().pose();
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
