package top.babyzombie.addons.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
import net.minecraft.world.phys.Vec3;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/** Draws filled boxes, wireframe boxes, and lines in the world using custom render pipelines. */
public final class WorldRenderUtils {

    private static final String MOD_ID = "babyzombieaddons";

    // ── Pipelines ──────────────────────────────────────────────
    private static final RenderPipeline FILLED_DEPTH = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/bza_filled_depth"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .build()
    );
    private static final RenderPipeline FILLED_NO_DEPTH = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/bza_filled_no_depth"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    );
    private static final RenderPipeline LINES_DEPTH = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/bza_lines_depth"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .build()
    );
    private static final RenderPipeline LINES_NO_DEPTH = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/bza_lines_no_depth"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
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

    // ═══════════════════════════════════════════════════════════════
    // Public API — Filled Box  (QUADS mode, solid 6-face box)
    // ═══════════════════════════════════════════════════════════════

    /** Draw a filled box from (x1,y1,z1) to (x2,y2,z2) with the given color and alpha. */
    public static void drawFilledBox(WorldRenderContext context, double x1, double y1, double z1,
                                      double x2, double y2, double z2,
                                      float r, float g, float b, float a, boolean depthTest) {
        var pipeline = depthTest ? FILLED_DEPTH : FILLED_NO_DEPTH;
        if (filledBuf == null) {
            filledBuf = new BufferBuilder(ALLOCATOR, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
        }
        var pose = applyCameraTransform(context);
        renderFilledBox(pose, filledBuf, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, r, g, b, a);
        context.matrices().popPose();
        uploadAndDrawFilled(pipeline, filledBuf);
        filledBuf = null;
    }

    // ═══════════════════════════════════════════════════════════════
    // Public API — Wireframe Box  (LINES_SNIPPET, 12 edge lines)
    // ═══════════════════════════════════════════════════════════════

    /** Draw a wireframe box from (x1,y1,z1) to (x2,y2,z2) with adjustable line width. */
    public static void drawWireframeBox(WorldRenderContext context, double x1, double y1, double z1,
                                         double x2, double y2, double z2,
                                         float r, float g, float b, float a,
                                         boolean depthTest, float lineWidth) {
        var pipeline = depthTest ? LINES_DEPTH : LINES_NO_DEPTH;
        if (linesBuf == null) {
            linesBuf = new BufferBuilder(ALLOCATOR, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
        }
        var pose = applyCameraTransform(context);
        renderWireframeBox(pose, linesBuf, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, r, g, b, a, lineWidth);
        context.matrices().popPose();
        uploadAndDrawLines(pipeline, linesBuf);
        linesBuf = null;
    }

    // ═══════════════════════════════════════════════════════════════
    // Public API — Line  (LINES_SNIPPET, single line segment)
    // ═══════════════════════════════════════════════════════════════

    /** Draw a line from (x1,y1,z1) to (x2,y2,z2) with adjustable line width. */
    public static void drawLine(WorldRenderContext context, double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 float r, float g, float b, float a,
                                 boolean depthTest, float lineWidth) {
        var pipeline = depthTest ? LINES_DEPTH : LINES_NO_DEPTH;
        if (linesBuf == null) {
            linesBuf = new BufferBuilder(ALLOCATOR, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
        }
        var pose = applyCameraTransform(context);
        addLineVertex(pose, linesBuf, (float) x1, (float) y1, (float) z1, r, g, b, a, lineWidth);
        addLineVertex(pose, linesBuf, (float) x2, (float) y2, (float) z2, r, g, b, a, lineWidth);
        context.matrices().popPose();
        uploadAndDrawLines(pipeline, linesBuf);
        linesBuf = null;
    }

    // ═══════════════════════════════════════════════════════════════
    // Vertex builders
    // ═══════════════════════════════════════════════════════════════

    private static void renderFilledBox(Matrix4fc pose, BufferBuilder buf,
                                         float minX, float minY, float minZ,
                                         float maxX, float maxY, float maxZ,
                                         float r, float g, float b, float a) {
        buf.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a);
        buf.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a);
        buf.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a);
        buf.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a);
        buf.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a);
        buf.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a);
        buf.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a);
        buf.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a);
        buf.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a);
        buf.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a);
        buf.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a);
        buf.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a);
        buf.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a);
        buf.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a);
        buf.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a);
        buf.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a);
        buf.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a);
        buf.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a);
        buf.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a);
        buf.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a);
        buf.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a);
        buf.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a);
        buf.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a);
        buf.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a);
    }

    private static void renderWireframeBox(Matrix4fc pose, BufferBuilder buf,
                                            float minX, float minY, float minZ,
                                            float maxX, float maxY, float maxZ,
                                            float r, float g, float b, float a, float lw) {
        addLineVertex(pose, buf, minX, minY, minZ, r, g, b, a, lw);
        addLineVertex(pose, buf, maxX, minY, minZ, r, g, b, a, lw);
        addLineVertex(pose, buf, maxX, minY, minZ, r, g, b, a, lw);
        addLineVertex(pose, buf, maxX, minY, maxZ, r, g, b, a, lw);
        addLineVertex(pose, buf, maxX, minY, maxZ, r, g, b, a, lw);
        addLineVertex(pose, buf, minX, minY, maxZ, r, g, b, a, lw);
        addLineVertex(pose, buf, minX, minY, maxZ, r, g, b, a, lw);
        addLineVertex(pose, buf, minX, minY, minZ, r, g, b, a, lw);
        addLineVertex(pose, buf, minX, maxY, minZ, r, g, b, a, lw);
        addLineVertex(pose, buf, maxX, maxY, minZ, r, g, b, a, lw);
        addLineVertex(pose, buf, maxX, maxY, minZ, r, g, b, a, lw);
        addLineVertex(pose, buf, maxX, maxY, maxZ, r, g, b, a, lw);
        addLineVertex(pose, buf, maxX, maxY, maxZ, r, g, b, a, lw);
        addLineVertex(pose, buf, minX, maxY, maxZ, r, g, b, a, lw);
        addLineVertex(pose, buf, minX, maxY, maxZ, r, g, b, a, lw);
        addLineVertex(pose, buf, minX, maxY, minZ, r, g, b, a, lw);
        addLineVertex(pose, buf, minX, minY, minZ, r, g, b, a, lw);
        addLineVertex(pose, buf, minX, maxY, minZ, r, g, b, a, lw);
        addLineVertex(pose, buf, maxX, minY, minZ, r, g, b, a, lw);
        addLineVertex(pose, buf, maxX, maxY, minZ, r, g, b, a, lw);
        addLineVertex(pose, buf, maxX, minY, maxZ, r, g, b, a, lw);
        addLineVertex(pose, buf, maxX, maxY, maxZ, r, g, b, a, lw);
        addLineVertex(pose, buf, minX, minY, maxZ, r, g, b, a, lw);
        addLineVertex(pose, buf, minX, maxY, maxZ, r, g, b, a, lw);
    }

    private static void addLineVertex(Matrix4fc pose, BufferBuilder buf, float x, float y, float z,
                                       float r, float g, float b, float a, float lw) {
        buf.addVertex(pose, x, y, z).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(lw);
    }

    // ═══════════════════════════════════════════════════════════════
    // Upload + Draw
    // ═══════════════════════════════════════════════════════════════

    private static void uploadAndDrawFilled(RenderPipeline pipeline, BufferBuilder buf) {
        MeshData builtBuffer = buf.buildOrThrow();
        MeshData.DrawState drawParameters = builtBuffer.drawState();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = uploadVertices(drawParameters, format, builtBuffer, true);
        draw(pipeline, builtBuffer, drawParameters, vertices, format);
        filledVertexBuffer.rotate();
        builtBuffer.close();
    }

    private static void uploadAndDrawLines(RenderPipeline pipeline, BufferBuilder buf) {
        MeshData builtBuffer = buf.buildOrThrow();
        MeshData.DrawState drawParameters = builtBuffer.drawState();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = uploadVertices(drawParameters, format, builtBuffer, false);
        draw(pipeline, builtBuffer, drawParameters, vertices, format);
        linesVertexBuffer.rotate();
        builtBuffer.close();
    }

    private static GpuBuffer uploadVertices(MeshData.DrawState drawParameters, VertexFormat format,
                                             MeshData builtBuffer, boolean filled) {
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();
        MappableRingBuffer ringBuffer = filled ? filledVertexBuffer : linesVertexBuffer;

        if (ringBuffer == null || ringBuffer.size() < vertexBufferSize) {
            if (ringBuffer != null) ringBuffer.close();
            String label = MOD_ID + (filled ? " filled" : " lines") + " render";
            ringBuffer = new MappableRingBuffer(() -> label,
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
            if (filled) filledVertexBuffer = ringBuffer;
            else linesVertexBuffer = ringBuffer;
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
                ringBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
        }

        return ringBuffer.currentBuffer();
    }

    private static void draw(RenderPipeline pipeline, MeshData builtBuffer,
                              MeshData.DrawState drawParameters, GpuBuffer vertices, VertexFormat format) {
        var client = Minecraft.getInstance();
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            builtBuffer.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.indexBuffer());
            indexType = drawParameters.indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer =
                RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.type();
        }

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> MOD_ID + " world render",
                    client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
                    client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);
            renderPass.drawIndexed(0, 0, drawParameters.indexCount(), 1);
        }

        builtBuffer.close();
    }

    // ═══════════════════════════════════════════════════════════════
    // Cleanup
    // ═══════════════════════════════════════════════════════════════

    /** Release all GPU buffers. Call on client shutdown. */
    public static void close() {
        ALLOCATOR.close();
        if (filledVertexBuffer != null) {
            filledVertexBuffer.close();
            filledVertexBuffer = null;
        }
        if (linesVertexBuffer != null) {
            linesVertexBuffer.close();
            linesVertexBuffer = null;
        }
    }
}
