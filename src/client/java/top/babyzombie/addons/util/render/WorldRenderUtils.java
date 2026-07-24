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

/**
 * 在世界空间中绘制各种形状的工具类。
 * <ul>
 *   <li>{@link #drawFilledBox}  — 实心方块（QUADS）</li>
 *   <li>{@link #drawWireframeBox} — 线框方块（LINES）</li>
 *   <li>{@link #drawLine}    — 单条线段（LINES）</li>
 *   <li>{@link #drawCircle}  — 线圈，可自定义朝向（LINES）</li>
 *   <li>{@link #drawFilledCircle} — 实心圆盘（TRIANGLES）</li>
 *   <li>{@link #drawFilledSphere} — 实心球体（TRIANGLES）</li>
 * </ul>
 */
public final class WorldRenderUtils {

    private static final String MOD_ID = "babyzombieaddons";

    // ── Pipelines ──────────────────────────────────────────────
    // 26.2 Vulkan: 深度比较用 GREATER_THAN_OR_EQUAL（Vulkan reversed-Z）
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
    // 三角形管线：基于 DEBUG_FILLED_SNIPPET（已含 cull=false），只改拓扑和深度
    private static final RenderPipeline TRIANGLES_DEPTH = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/bza_triangles_depth"))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build()
    );
    private static final RenderPipeline TRIANGLES_NO_DEPTH = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/bza_triangles_no_depth"))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withDepthStencilState(Optional.empty())
            .build()
    );

    // ── Buffer management ──────────────────────────────────────
    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(1536);
    private static MappableRingBuffer filledVertexBuffer;
    private static MappableRingBuffer linesVertexBuffer;
    private static MappableRingBuffer trianglesVertexBuffer;
    private static BufferBuilder filledBuf;
    private static BufferBuilder linesBuf;
    private static BufferBuilder trianglesBuf;

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
    // Public API — Filled Box  (QUADS, 实心六面体)
    // ═══════════════════════════════════════════════════════════════

    /**
     * 在世界中绘制一个实心半透明方块。
     *
     * @param context   世界渲染上下文（从渲染事件获取）
     * @param x1        最小 X 坐标
     * @param y1        最小 Y 坐标
     * @param z1        最小 Z 坐标
     * @param x2        最大 X 坐标
     * @param y2        最大 Y 坐标
     * @param z2        最大 Z 坐标
     * @param r         红色分量 [0, 1]
     * @param g         绿色分量 [0, 1]
     * @param b         蓝色分量 [0, 1]
     * @param a         透明度 [0, 1]，0 为完全透明
     * @param depthTest 是否启用深度测试，{@code true} 时方块会被障碍物遮挡
     */
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
    // Public API — Wireframe Box  (LINES, 12 条棱边)
    // ═══════════════════════════════════════════════════════════════

    /**
     * 在世界中绘制一个线框方块（仅 12 条棱边，不填充面）。
     *
     * @param context   世界渲染上下文
     * @param x1        最小 X 坐标
     * @param y1        最小 Y 坐标
     * @param z1        最小 Z 坐标
     * @param x2        最大 X 坐标
     * @param y2        最大 Y 坐标
     * @param z2        最大 Z 坐标
     * @param r         红色分量 [0, 1]
     * @param g         绿色分量 [0, 1]
     * @param b         蓝色分量 [0, 1]
     * @param a         透明度 [0, 1]
     * @param depthTest 是否启用深度测试
     * @param lineWidth 线宽（像素），默认约 1.0
     */
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
    // Public API — Line  (LINES, 单条线段)
    // ═══════════════════════════════════════════════════════════════

    /**
     * 在世界中绘制一条线段。
     *
     * @param context   世界渲染上下文
     * @param x1        起点 X
     * @param y1        起点 Y
     * @param z1        起点 Z
     * @param x2        终点 X
     * @param y2        终点 Y
     * @param z2        终点 Z
     * @param r         红色分量 [0, 1]
     * @param g         绿色分量 [0, 1]
     * @param b         蓝色分量 [0, 1]
     * @param a         透明度 [0, 1]
     * @param depthTest 是否启用深度测试
     * @param lineWidth 线宽（像素）
     */
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
    // Public API — Circle  (LINES, 默认水平/XZ 平面)
    // ═══════════════════════════════════════════════════════════════

    /**
     * 在 XZ 平面（水平/地面）上绘制一个线圈。默认 64 段。
     *
     * @param context  世界渲染上下文
     * @param centerX  圆心 X
     * @param centerY  圆心 Y（高度）
     * @param centerZ  圆心 Z
     * @param radius   半径
     * @param r        红色分量 [0, 1]
     * @param g        绿色分量 [0, 1]
     * @param b        蓝色分量 [0, 1]
     * @param a        透明度 [0, 1]
     * @param depthTest 是否启用深度测试
     * @param lineWidth 线宽（像素）
     */
    public static void drawCircle(WorldRenderContext context, double centerX, double centerY, double centerZ,
                                   double radius, float r, float g, float b, float a,
                                   boolean depthTest, float lineWidth) {
        drawCircle(context, centerX, centerY, centerZ, radius, r, g, b, a, depthTest, lineWidth, 64);
    }

    /**
     * 在 XZ 平面（水平/地面）上绘制一个线圈，可指定段数。
     *
     * @param segments 线段数量，越多越圆滑（默认 64）
     */
    public static void drawCircle(WorldRenderContext context, double centerX, double centerY, double centerZ,
                                   double radius, float r, float g, float b, float a,
                                   boolean depthTest, float lineWidth, int segments) {
        drawCircleWithNormal(context, centerX, centerY, centerZ, radius,
                r, g, b, a, depthTest, lineWidth, segments, 0, 1, 0);
    }

    // ═══════════════════════════════════════════════════════════════
    // Public API — Circle with arbitrary normal
    // ═══════════════════════════════════════════════════════════════

    /**
     * 在任意朝向的平面上绘制线圈。
     *
     * @param nx 法向量 X 分量（无需归一化）
     * @param ny 法向量 Y 分量（无需归一化）
     * @param nz 法向量 Z 分量（无需归一化）
     *           <p>例如 {@code (0,1,0)} 为水平面（默认），{@code (0,0,1)} 为竖直面朝 Z。
     *           任意方向都支持，可实现倾斜/旋转的圆。</p>
     */
    public static void drawCircle(WorldRenderContext context, double centerX, double centerY, double centerZ,
                                   double radius, float r, float g, float b, float a,
                                   boolean depthTest, float lineWidth,
                                   float nx, float ny, float nz) {
        drawCircle(context, centerX, centerY, centerZ, radius, r, g, b, a, depthTest, lineWidth, 64, nx, ny, nz);
    }

    /**
     * 在任意朝向的平面上绘制线圈，可指定段数。
     *
     * @param segments 线段数量，越多越圆滑
     * @param nx       法向量 X 分量
     * @param ny       法向量 Y 分量
     * @param nz       法向量 Z 分量
     */
    public static void drawCircle(WorldRenderContext context, double centerX, double centerY, double centerZ,
                                   double radius, float r, float g, float b, float a,
                                   boolean depthTest, float lineWidth, int segments,
                                   float nx, float ny, float nz) {
        drawCircleWithNormal(context, centerX, centerY, centerZ, radius,
                r, g, b, a, depthTest, lineWidth, segments, nx, ny, nz);
    }

    // ═══════════════════════════════════════════════════════════════
    // Public API — Filled Circle  (TRIANGLES, 三角形扇)
    // ═══════════════════════════════════════════════════════════════

    /**
     * 在 XZ 平面（水平/地面）上绘制一个实心半透明圆盘。默认 64 段。
     *
     * @param context  世界渲染上下文
     * @param centerX  圆心 X
     * @param centerY  圆心 Y（高度）
     * @param centerZ  圆心 Z
     * @param radius   半径
     * @param r        红色分量 [0, 1]
     * @param g        绿色分量 [0, 1]
     * @param b        蓝色分量 [0, 1]
     * @param a        透明度 [0, 1]
     * @param depthTest 是否启用深度测试
     */
    public static void drawFilledCircle(WorldRenderContext context,
                                         double centerX, double centerY, double centerZ,
                                         double radius, float r, float g, float b, float a,
                                         boolean depthTest) {
        drawFilledCircle(context, centerX, centerY, centerZ, radius,
                r, g, b, a, depthTest, 64, 0, 1, 0);
    }

    /**
     * 在任意朝向的平面上绘制实心圆盘。
     *
     * @param nx 法向量 X 分量（无需归一化）
     * @param ny 法向量 Y 分量
     * @param nz 法向量 Z 分量
     */
    public static void drawFilledCircle(WorldRenderContext context,
                                         double centerX, double centerY, double centerZ,
                                         double radius, float r, float g, float b, float a,
                                         boolean depthTest,
                                         float nx, float ny, float nz) {
        drawFilledCircle(context, centerX, centerY, centerZ, radius,
                r, g, b, a, depthTest, 64, nx, ny, nz);
    }

    /**
     * 在任意朝向的平面上绘制实心圆盘，可指定段数。
     *
     * @param segments 边缘段数，越多越圆滑（默认 64）
     * @param nx       法向量 X 分量
     * @param ny       法向量 Y 分量
     * @param nz       法向量 Z 分量
     */
    public static void drawFilledCircle(WorldRenderContext context,
                                         double centerX, double centerY, double centerZ,
                                         double radius, float r, float g, float b, float a,
                                         boolean depthTest, int segments,
                                         float nx, float ny, float nz) {
        var pipeline = depthTest ? TRIANGLES_DEPTH : TRIANGLES_NO_DEPTH;
        var format = pipeline.getVertexFormatBinding(0);
        if (format == null) return;
        if (trianglesBuf == null) {
            trianglesBuf = new BufferBuilder(ALLOCATOR, PrimitiveTopology.TRIANGLES, format);
        }
        var pose = applyCameraTransform(context);

        Vector3f u = new Vector3f();
        Vector3f v = new Vector3f();
        computeCircleBasis(nx, ny, nz, u, v);

        float cx = (float) centerX;
        float cy = (float) centerY;
        float cz = (float) centerZ;
        float rad = (float) radius;

        double angleStep = 2.0 * Math.PI / segments;
        for (int i = 0; i < segments; i++) {
            double a1 = i * angleStep;
            double a2 = (i + 1) * angleStep;

            float ex1 = (float)(rad * Math.cos(a1));
            float ey1 = (float)(rad * Math.sin(a1));
            float ex2 = (float)(rad * Math.cos(a2));
            float ey2 = (float)(rad * Math.sin(a2));

            // Triangle: center → edge_i → edge_{i+1}
            addTriVertex(pose, trianglesBuf, cx, cy, cz, r, g, b, a);
            addTriVertex(pose, trianglesBuf,
                    cx + ex1 * u.x + ey1 * v.x,
                    cy + ex1 * u.y + ey1 * v.y,
                    cz + ex1 * u.z + ey1 * v.z,
                    r, g, b, a);
            addTriVertex(pose, trianglesBuf,
                    cx + ex2 * u.x + ey2 * v.x,
                    cy + ex2 * u.y + ey2 * v.y,
                    cz + ex2 * u.z + ey2 * v.z,
                    r, g, b, a);
        }

        context.matrices().popPose();
        uploadAndDrawTriangles(pipeline, trianglesBuf);
        trianglesBuf = null;
    }

    // ═══════════════════════════════════════════════════════════════
    // Public API — Filled Sphere  (TRIANGLES, UV 球体网格)
    // ═══════════════════════════════════════════════════════════════

    /**
     * 在世界中绘制一个实心半透明球体（UV 球体网格，三角形渲染）。
     * 默认 16 层（纬度）× 32 段（经度）。
     *
     * @param context   世界渲染上下文
     * @param centerX   球心 X
     * @param centerY   球心 Y
     * @param centerZ   球心 Z
     * @param radius    半径
     * @param r         红色分量 [0, 1]
     * @param g         绿色分量 [0, 1]
     * @param b         蓝色分量 [0, 1]
     * @param a         透明度 [0, 1]
     * @param depthTest 是否启用深度测试
     */
    public static void drawFilledSphere(WorldRenderContext context,
                                         double centerX, double centerY, double centerZ,
                                         double radius, float r, float g, float b, float a,
                                         boolean depthTest) {
        drawFilledSphere(context, centerX, centerY, centerZ, radius, r, g, b, a, depthTest, 16, 32);
    }

    /**
     * 在世界中绘制一个实心半透明球体，可控制精度。
     *
     * @param stacks 纬度层数（南北极之间的分层），越大越光滑（默认 16）
     * @param slices 经度段数（绕赤道的分段），越大越光滑（默认 32）
     */
    public static void drawFilledSphere(WorldRenderContext context,
                                         double centerX, double centerY, double centerZ,
                                         double radius, float r, float g, float b, float a,
                                         boolean depthTest, int stacks, int slices) {
        var pipeline = depthTest ? TRIANGLES_DEPTH : TRIANGLES_NO_DEPTH;
        var format = pipeline.getVertexFormatBinding(0);
        if (format == null) return;
        if (trianglesBuf == null) {
            trianglesBuf = new BufferBuilder(ALLOCATOR, PrimitiveTopology.TRIANGLES, format);
        }
        var pose = applyCameraTransform(context);
        renderSphere(pose, trianglesBuf, (float) centerX, (float) centerY, (float) centerZ,
                (float) radius, r, g, b, a, stacks, slices);
        context.matrices().popPose();
        uploadAndDrawTriangles(pipeline, trianglesBuf);
        trianglesBuf = null;
    }

    // ═══════════════════════════════════════════════════════════════
    // Circle internals
    // ═══════════════════════════════════════════════════════════════

    /** Core circle — vertices on the plane ⟂ normal, rendered as line segments. */
    private static void drawCircleWithNormal(WorldRenderContext context,
                                              double cx, double cy, double cz, double radius,
                                              float r, float g, float b, float a,
                                              boolean depthTest, float lineWidth, int segments,
                                              float nx, float ny, float nz) {
        var pipeline = depthTest ? LINES_DEPTH : LINES_NO_DEPTH;
        var format = pipeline.getVertexFormatBinding(0);
        if (format == null) return;
        if (linesBuf == null) {
            linesBuf = new BufferBuilder(ALLOCATOR, PrimitiveTopology.LINES, format);
        }
        var pose = applyCameraTransform(context);

        Vector3f u = new Vector3f();
        Vector3f v = new Vector3f();
        computeCircleBasis(nx, ny, nz, u, v);

        double angleStep = 2.0 * Math.PI / segments;
        for (int i = 0; i < segments; i++) {
            double a1 = i * angleStep;
            double a2 = (i + 1) * angleStep;
            double cos1 = Math.cos(a1);
            double sin1 = Math.sin(a1);
            double cos2 = Math.cos(a2);
            double sin2 = Math.sin(a2);

            float x1 = (float)(cx + radius * (cos1 * u.x + sin1 * v.x));
            float y1 = (float)(cy + radius * (cos1 * u.y + sin1 * v.y));
            float z1 = (float)(cz + radius * (cos1 * u.z + sin1 * v.z));
            float x2 = (float)(cx + radius * (cos2 * u.x + sin2 * v.x));
            float y2 = (float)(cy + radius * (cos2 * u.y + sin2 * v.y));
            float z2 = (float)(cz + radius * (cos2 * u.z + sin2 * v.z));

            addLineVertex(pose, linesBuf, x1, y1, z1, r, g, b, a, lineWidth);
            addLineVertex(pose, linesBuf, x2, y2, z2, r, g, b, a, lineWidth);
        }

        context.matrices().popPose();
        uploadAndDrawLines(pipeline, linesBuf);
        linesBuf = null;
    }

    /** Compute two orthonormal tangent vectors (u, v) spanning the plane ⟂ normal. */
    private static void computeCircleBasis(float nx, float ny, float nz, Vector3f u, Vector3f v) {
        float ax, ay, az;
        if (Math.abs(nx) < 0.9f) {
            ax = 1f; ay = 0f; az = 0f;
        } else {
            ax = 0f; ay = 1f; az = 0f;
        }

        float ux = ay * nz - az * ny;
        float uy = az * nx - ax * nz;
        float uz = ax * ny - ay * nx;
        float uLen = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        u.set(ux / uLen, uy / uLen, uz / uLen);

        v.set(ny * uz - nz * uy, nz * ux - nx * uz, nx * uy - ny * ux);
    }

    // ═══════════════════════════════════════════════════════════════
    // Sphere vertex builder
    // ═══════════════════════════════════════════════════════════════

    /** Build UV-sphere mesh as triangle list. */
    private static void renderSphere(Matrix4fc pose, BufferBuilder buf,
                                      float cx, float cy, float cz, float radius,
                                      float r, float g, float b, float a,
                                      int stacks, int slices) {
        for (int i = 0; i < stacks; i++) {
            double phi1 = Math.PI * i / stacks;
            double phi2 = Math.PI * (i + 1) / stacks;

            for (int j = 0; j < slices; j++) {
                double theta1 = 2.0 * Math.PI * j / slices;
                double theta2 = 2.0 * Math.PI * (j + 1) / slices;

                float x1 = (float)(cx + radius * Math.sin(phi1) * Math.cos(theta1));
                float y1 = (float)(cy + radius * Math.cos(phi1));
                float z1 = (float)(cz + radius * Math.sin(phi1) * Math.sin(theta1));

                float x2 = (float)(cx + radius * Math.sin(phi2) * Math.cos(theta1));
                float y2 = (float)(cy + radius * Math.cos(phi2));
                float z2 = (float)(cz + radius * Math.sin(phi2) * Math.sin(theta1));

                float x3 = (float)(cx + radius * Math.sin(phi2) * Math.cos(theta2));
                float y3 = (float)(cy + radius * Math.cos(phi2));
                float z3 = (float)(cz + radius * Math.sin(phi2) * Math.sin(theta2));

                float x4 = (float)(cx + radius * Math.sin(phi1) * Math.cos(theta2));
                float y4 = (float)(cy + radius * Math.cos(phi1));
                float z4 = (float)(cz + radius * Math.sin(phi1) * Math.sin(theta2));

                // Triangle 1: (i,j) → (i+1,j) → (i+1,j+1)
                addTriVertex(pose, buf, x1, y1, z1, r, g, b, a);
                addTriVertex(pose, buf, x2, y2, z2, r, g, b, a);
                addTriVertex(pose, buf, x3, y3, z3, r, g, b, a);

                // Triangle 2: (i,j) → (i+1,j+1) → (i,j+1)
                addTriVertex(pose, buf, x1, y1, z1, r, g, b, a);
                addTriVertex(pose, buf, x3, y3, z3, r, g, b, a);
                addTriVertex(pose, buf, x4, y4, z4, r, g, b, a);
            }
        }
    }

    private static void addTriVertex(Matrix4fc pose, BufferBuilder buf,
                                      float x, float y, float z,
                                      float r, float g, float b, float a) {
        buf.addVertex(pose, x, y, z).setColor(r, g, b, a);
    }

    // ═══════════════════════════════════════════════════════════════
    // Box / Line vertex builders
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

        GpuBuffer vertices = uploadVertices(format, builtBuffer, 0);
        draw(pipeline, builtBuffer, drawParameters, vertices);
        filledVertexBuffer.rotate();
        builtBuffer.close();
    }

    private static void uploadAndDrawLines(RenderPipeline pipeline, BufferBuilder buf) {
        MeshData builtBuffer = buf.buildOrThrow();
        MeshData.DrawState drawParameters = builtBuffer.drawState();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = uploadVertices(format, builtBuffer, 1);
        draw(pipeline, builtBuffer, drawParameters, vertices);
        linesVertexBuffer.rotate();
        builtBuffer.close();
    }

    private static void uploadAndDrawTriangles(RenderPipeline pipeline, BufferBuilder buf) {
        MeshData builtBuffer = buf.buildOrThrow();
        MeshData.DrawState drawParameters = builtBuffer.drawState();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = uploadVertices(format, builtBuffer, 2);
        draw(pipeline, builtBuffer, drawParameters, vertices);
        trianglesVertexBuffer.rotate();
        builtBuffer.close();
    }

    /** MC 26.2: writeToBuffer. @param kind 0=filled, 1=lines, 2=triangles */
    private static GpuBuffer uploadVertices(VertexFormat format, MeshData builtBuffer, int kind) {
        int vertexBufferSize = builtBuffer.drawState().vertexCount() * format.getVertexSize();
        MappableRingBuffer ringBuffer = switch (kind) {
            case 0 -> filledVertexBuffer;
            case 1 -> linesVertexBuffer;
            default -> trianglesVertexBuffer;
        };

        if (ringBuffer == null || ringBuffer.size() < vertexBufferSize) {
            if (ringBuffer != null) ringBuffer.close();
            String label = MOD_ID + switch (kind) {
                case 0 -> " filled";
                case 1 -> " lines";
                default -> " triangles";
            } + " render";
            ringBuffer = new MappableRingBuffer(() -> label,
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_DST, vertexBufferSize);
            switch (kind) {
                case 0 -> filledVertexBuffer = ringBuffer;
                case 1 -> linesVertexBuffer = ringBuffer;
                default -> trianglesVertexBuffer = ringBuffer;
            }
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

        // MC 26.2: getSequentialBuffer accepts PrimitiveTopology
        RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer =
            RenderSystem.getSequentialBuffer(pipeline.getPrimitiveTopology());
        GpuBuffer indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
        IndexType indexType = shapeIndexBuffer.type();

        // MC 26.2: getModelViewMatrixCopy()
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
            // MC 26.2: setVertexBuffer takes GpuBufferSlice
            renderPass.setVertexBuffer(0, vertices.slice());
            renderPass.setIndexBuffer(indices, indexType);
            // MC 26.2: drawIndexed(count, instances, firstIndex, baseVertex, vertexOffset)
            renderPass.drawIndexed(drawParameters.indexCount(), 1, 0, 0, 0);
        }

        builtBuffer.close();
    }

    // ═══════════════════════════════════════════════════════════════
    // Cleanup
    // ═══════════════════════════════════════════════════════════════

    /** 释放所有 GPU 缓冲区。应在客户端关闭时调用。 */
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
        if (trianglesVertexBuffer != null) {
            trianglesVertexBuffer.close();
            trianglesVertexBuffer = null;
        }
    }
}
