package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
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
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.OptionalDouble;

/** Draws filled boxes, wireframe boxes, and lines in the world using custom render pipelines. */
public final class WorldRenderUtils {

    private static final String MOD_ID = "babyzombieaddons";

    // ── Pipelines ──────────────────────────────────────────────
    // 26.2 Vulkan: 深度比较用 GREATER_THAN_OR_EQUAL（不再用 LESS_THAN_OR_EQUAL）
    private static final RenderPipeline FILLED_DEPTH = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/bza_filled_depth"))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
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
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
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

    // ═══════════════════════════════════════════════════════════════
    // Public API — Filled Box
    // ═══════════════════════════════════════════════════════════════

    public static void drawFilledBox(WorldRenderContext context, double x1, double y1, double z1,
                                      double x2, double y2, double z2,
                                      float r, float g, float b, float a, boolean depthTest) {
        var pipeline = depthTest ? FILLED_DEPTH : FILLED_NO_DEPTH;
        var format = pipeline.getVertexFormatBinding(0);
        if (format == null) return;
        if (filledBuf == null) {
            filledBuf = new BufferBuilder(ALLOCATOR, PrimitiveTopology.QUADS, format);
        }
        var pose = applyCameraTransform(context);
        renderFilledBox(pose, filledBuf, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, r, g, b, a);
        context.matrices().popPose();
        uploadAndDrawFilled(pipeline, filledBuf);
        filledBuf = null;
    }

    // ═══════════════════════════════════════════════════════════════
    // Public API — Wireframe Box
    // ═══════════════════════════════════════════════════════════════

    public static void drawWireframeBox(WorldRenderContext context, double x1, double y1, double z1,
                                         double x2, double y2, double z2,
                                         float r, float g, float b, float a,
                                         boolean depthTest, float lineWidth) {
        var pipeline = depthTest ? LINES_DEPTH : LINES_NO_DEPTH;
        var format = pipeline.getVertexFormatBinding(0);
        if (format == null) return;
        if (linesBuf == null) {
            linesBuf = new BufferBuilder(ALLOCATOR, PrimitiveTopology.LINES, format);
        }
        var pose = applyCameraTransform(context);
        renderWireframeBox(pose, linesBuf, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, r, g, b, a, lineWidth);
        context.matrices().popPose();
        uploadAndDrawLines(pipeline, linesBuf);
        linesBuf = null;
    }

    // ═══════════════════════════════════════════════════════════════
    // Public API — Line
    // ═══════════════════════════════════════════════════════════════

    public static void drawLine(WorldRenderContext context, double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 float r, float g, float b, float a,
                                 boolean depthTest, float lineWidth) {
        var pipeline = depthTest ? LINES_DEPTH : LINES_NO_DEPTH;
        var format = pipeline.getVertexFormatBinding(0);
        if (format == null) return;
        if (linesBuf == null) {
            linesBuf = new BufferBuilder(ALLOCATOR, PrimitiveTopology.LINES, format);
        }
        var pose = applyCameraTransform(context);
        addLineVertex(pose, linesBuf, (float) x1, (float) y1, (float) z1, r, g, b, a, lineWidth);
        addLineVertex(pose, linesBuf, (float) x2, (float) y2, (float) z2, r, g, b, a, lineWidth);
        context.matrices().popPose();
        uploadAndDrawLines(pipeline, linesBuf);
        linesBuf = null;
    }

    // ═══════════════════════════════════════════════════════════════
    // Public API — Circle  (LINES_SNIPPET, ground circle in XZ plane)
    // ═══════════════════════════════════════════════════════════════

    /** Draw a circle on the XZ plane (horizontal / on the ground) with the given radius. */
    public static void drawCircle(WorldRenderContext context, double centerX, double centerY, double centerZ,
                                   double radius, float r, float g, float b, float a,
                                   boolean depthTest, float lineWidth) {
        drawCircle(context, centerX, centerY, centerZ, radius, r, g, b, a, depthTest, lineWidth, 64);
    }

    /** Draw a circle on the XZ plane with a custom number of segments. */
    public static void drawCircle(WorldRenderContext context, double centerX, double centerY, double centerZ,
                                   double radius, float r, float g, float b, float a,
                                   boolean depthTest, float lineWidth, int segments) {
        var pipeline = depthTest ? LINES_DEPTH : LINES_NO_DEPTH;
        if (linesBuf == null) {
            linesBuf = new BufferBuilder(ALLOCATOR, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
        }
        var pose = applyCameraTransform(context);

        double angleStep = 2.0 * Math.PI / segments;
        for (int i = 0; i < segments; i++) {
            double a1 = i * angleStep;
            double a2 = (i + 1) * angleStep;
            float x1 = (float)(centerX + radius * Math.cos(a1));
            float z1 = (float)(centerZ + radius * Math.sin(a1));
            float x2 = (float)(centerX + radius * Math.cos(a2));
            float z2 = (float)(centerZ + radius * Math.sin(a2));
            addLineVertex(pose, linesBuf, x1, (float)centerY, z1, r, g, b, a, lineWidth);
            addLineVertex(pose, linesBuf, x2, (float)centerY, z2, r, g, b, a, lineWidth);
        }

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

        GpuBuffer vertices = uploadVertices(format, builtBuffer, true);
        draw(pipeline, builtBuffer, drawParameters, vertices);
        filledVertexBuffer.rotate();
        builtBuffer.close();
    }

    private static void uploadAndDrawLines(RenderPipeline pipeline, BufferBuilder buf) {
        MeshData builtBuffer = buf.buildOrThrow();
        MeshData.DrawState drawParameters = builtBuffer.drawState();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = uploadVertices(format, builtBuffer, false);
        draw(pipeline, builtBuffer, drawParameters, vertices);
        linesVertexBuffer.rotate();
        builtBuffer.close();
    }

    /** MC 26.2: mapBuffer → writeToBuffer */
    private static GpuBuffer uploadVertices(VertexFormat format, MeshData builtBuffer, boolean filled) {
        int vertexBufferSize = builtBuffer.drawState().vertexCount() * format.getVertexSize();
        MappableRingBuffer ringBuffer = filled ? filledVertexBuffer : linesVertexBuffer;

        if (ringBuffer == null || ringBuffer.size() < vertexBufferSize) {
            if (ringBuffer != null) ringBuffer.close();
            String label = MOD_ID + (filled ? " filled" : " lines") + " render";
            ringBuffer = new MappableRingBuffer(() -> label,
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_DST, vertexBufferSize);
            if (filled) filledVertexBuffer = ringBuffer;
            else linesVertexBuffer = ringBuffer;
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        commandEncoder.writeToBuffer(
                ringBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()),
                builtBuffer.vertexBuffer());

        return ringBuffer.currentBuffer();
    }

    private static void draw(RenderPipeline pipeline, MeshData builtBuffer,
                              MeshData.DrawState drawParameters, GpuBuffer vertices) {
        var client = Minecraft.getInstance();
        var mainTarget = client.gameRenderer.mainRenderTarget();
        var colorView = mainTarget.getColorTextureView();
        if (colorView == null) {
            builtBuffer.close();
            return;
        }

        // MC 26.2: getSequentialBuffer now accepts PrimitiveTopology
        RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer =
            RenderSystem.getSequentialBuffer(pipeline.getPrimitiveTopology());
        GpuBuffer indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
        IndexType indexType = shapeIndexBuffer.type();

        // MC 26.2: getModelViewMatrix() → getModelViewMatrixCopy()
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> MOD_ID + " world render",
                    colorView, Optional.empty(),
                    mainTarget.useDepth ? mainTarget.getDepthTextureView() : null,
                    OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            // MC 26.2: setVertexBuffer takes GpuBufferSlice, not GpuBuffer
            renderPass.setVertexBuffer(0, vertices.slice());
            renderPass.setIndexBuffer(indices, indexType);
            // MC 26.2: drawIndexed takes 5 args: (indexCount, instanceCount, firstIndex, baseVertex, vertexOffset)
            renderPass.drawIndexed(drawParameters.indexCount(), 1, 0, 0, 0);
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
