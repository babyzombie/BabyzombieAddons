package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.PrimitiveTopology;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

public final class BzaRenderer {
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static final StagedVertexBuffer VERTEX_BUFFER = new StagedVertexBuffer(
        () -> "BZA World Renderer", net.minecraft.client.renderer.rendertype.RenderType.SMALL_BUFFER_SIZE);
    private static final List<Draw> DRAWS = new ArrayList<>();

    private static StagedVertexBuffer.Draw currentDraw;
    private static RenderPipeline currentPipeline;

    static {
        LevelRenderEvents.END_MAIN.register(ctx -> {
            if (DRAWS.isEmpty()) return;
            var main = CLIENT.gameRenderer.mainRenderTarget();
            if (main.getColorTextureView() == null) return;

            VERTEX_BUFFER.upload();
            var enc = RenderSystem.getDevice().createCommandEncoder();
            try (var rp = enc.createRenderPass(() -> "bza_world", main.getColorTextureView(),
                    Optional.empty(), main.getDepthTextureView(), OptionalDouble.empty())) {
                for (var d : DRAWS) {
                    var info = VERTEX_BUFFER.getExecuteInfo(d.draw);
                    if (info == null) continue;
                    rp.setPipeline(d.pipeline);
                    RenderSystem.bindDefaultUniforms(rp);
                    rp.setUniform("DynamicTransforms",
                        RenderSystem.getDynamicUniforms()
                            .writeTransform(RenderSystem.getModelViewMatrixCopy(),
                                new Vector4f(1,1,1,1)));
                    rp.setVertexBuffer(0, info.vertexBuffer().slice());
                    rp.setIndexBuffer(info.indexBuffer(), info.indexType());
                    rp.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
                }
            }
            enc.submit();
            VERTEX_BUFFER.endDraw();
            VERTEX_BUFFER.endFrame();
            DRAWS.clear();
            currentDraw = null;
            currentPipeline = null;
        });
    }

    public static VertexConsumer begin(RenderPipeline pipeline, PrimitiveTopology topology) {
        if (currentDraw == null || pipeline != currentPipeline) {
            currentPipeline = pipeline;
            currentDraw = VERTEX_BUFFER.appendDraw(pipeline.getVertexFormatBinding(0), topology);
            DRAWS.add(new Draw(currentDraw, pipeline));
        }
        return VERTEX_BUFFER.getVertexBuilder(currentDraw);
    }

    public static Matrix4f cameraMatrix(CameraRenderState cam) {
        return new Matrix4f().translate((float)-cam.pos.x, (float)-cam.pos.y, (float)-cam.pos.z);
    }

    public static RenderPipeline register(String name, RenderPipeline.Snippet snippet, boolean throughWalls) {
        return RenderPipelines.register(
            com.mojang.blaze3d.pipeline.RenderPipeline.builder(snippet)
                .withLocation(Identifier.fromNamespaceAndPath("babyzombieaddons", "pipeline/" + name))
                .withDepthStencilState(throughWalls ? Optional.empty()
                    : java.util.Optional.of(new com.mojang.blaze3d.pipeline.DepthStencilState(
                        com.mojang.blaze3d.platform.CompareOp.LESS_THAN_OR_EQUAL, true)))
                .build());
    }

    public static void close() { VERTEX_BUFFER.close(); }

    private record Draw(StagedVertexBuffer.Draw draw, RenderPipeline pipeline) {}
}
