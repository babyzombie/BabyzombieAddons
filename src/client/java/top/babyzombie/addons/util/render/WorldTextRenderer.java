package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

import java.util.Optional;
import java.util.OptionalDouble;

/** 在世界上渲染浮空文字。MC 26.2: 使用 StagedVertexBuffer + Vulkan 帧图管线。 */
public final class WorldTextRenderer {

    private static final StagedVertexBuffer VERTEX_BUFFER = new StagedVertexBuffer(
            () -> "BabyzombieAddons Text", 131072
    );

    private WorldTextRenderer() {}

    public static void renderString(WorldRenderContext context, String text, double x, double y, double z,
                                     int color, float scale, boolean throughWalls) {
        renderString(context, text, x, y, z, color, scale, throughWalls, 0);
    }

    public static void renderString(WorldRenderContext context, String text, double x, double y, double z,
                                     int color, float scale, boolean throughWalls, float fontYOffset) {
        var client = Minecraft.getInstance();
        var font = client.font;
        var cameraState = context.worldState().cameraRenderState;
        var pipeline = throughWalls ? RenderPipelines.TEXT_SEE_THROUGH : RenderPipelines.TEXT;

        // 世界空间位置矩阵
        var positionMatrix = new Matrix4f()
                .translate((float) (x - cameraState.pos.x()), (float) (y - cameraState.pos.y()), (float) (z - cameraState.pos.z()))
                .rotate(cameraState.orientation)
                .scale(scale, -scale, scale);

        // 准备文字
        var preparedText = font.prepareText(
                Component.literal(text).getVisualOrderText(),
                -font.width(text) / 2f, fontYOffset,
                color, false, false, 0
        );

        // 收集 glyph 的纹理引用（第一个即可，所有 glyph 共用字体纹理）
        var texRef = new Object() { GpuTextureView textureView; };
        preparedText.visit(new Font.GlyphVisitor() {
            @Override public void acceptRenderable(TextRenderable r) {
                if (texRef.textureView == null) texRef.textureView = r.textureView();
            }
        });

        // 创建 draw
        var binding = pipeline.getVertexFormatBinding(0);
        if (binding == null) return;
        var draw = VERTEX_BUFFER.appendDraw(binding, pipeline.getPrimitiveTopology());

        // 遍历并渲染每个 glyph 到 CPU 端 buffer
        preparedText.visit(new Font.GlyphVisitor() {
            @Override
            public void acceptGlyph(TextRenderable.Styled glyph) {
                VertexConsumer buffer = VERTEX_BUFFER.getVertexBuilder(draw);
                glyph.render(positionMatrix, buffer, 0xF000F0, false);
            }

            @Override
            public void acceptEffect(TextRenderable bakedGlyph) {
                VertexConsumer buffer = VERTEX_BUFFER.getVertexBuilder(draw);
                bakedGlyph.render(positionMatrix, buffer, 0xF000F0, false);
            }
        });

        // 上传几何数据到 GPU
        VERTEX_BUFFER.upload();

        // 创建 render pass 并绘制
        var mainTarget = client.gameRenderer.mainRenderTarget();
        var colorView = mainTarget.getColorTextureView();
        if (colorView == null) return;
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "BabyzombieAddons WorldText",
                colorView, Optional.empty(),
                mainTarget.useDepth ? mainTarget.getDepthTextureView() : null,
                OptionalDouble.empty())) {

            RenderSystem.bindDefaultUniforms(renderPass);
            RenderSystem.getModelViewStack().pushMatrix();

            var execInfo = VERTEX_BUFFER.getExecuteInfo(draw);
            if (execInfo != null && texRef.textureView != null) {
                renderPass.setPipeline(pipeline);
                renderPass.setUniform("DynamicTransforms",
                        RenderSystem.getDynamicUniforms().writeTransform(
                                RenderSystem.getModelViewMatrixCopy(),
                                new org.joml.Vector4f(1, 1, 1, 1)
                        ));
                renderPass.bindTexture("Sampler0", texRef.textureView,
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                renderPass.setVertexBuffer(0, execInfo.vertexBuffer().slice());
                renderPass.setIndexBuffer(execInfo.indexBuffer(), execInfo.indexType());
                renderPass.drawIndexed(execInfo.indexCount(), 1, execInfo.firstIndex(), execInfo.baseVertex(), 0);
            }

            RenderSystem.getModelViewStack().popMatrix();
        }

        VERTEX_BUFFER.endDraw();
        VERTEX_BUFFER.endFrame();
    }
}
