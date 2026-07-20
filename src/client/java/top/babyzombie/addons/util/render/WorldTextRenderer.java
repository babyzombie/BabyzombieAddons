package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.Optional;
import java.util.OptionalDouble;

/** 在世界上渲染浮空文字。参考 Skyblocker TextPrimitiveRenderer。 */
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
        // 26.2: TEXT 管道在自定义 render pass 中深度测试不兼容，统一用 TEXT_SEE_THROUGH
var pipeline = RenderPipelines.TEXT_SEE_THROUGH;

        // 世界空间位置矩阵（同 Skyblocker）
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

        // 收集第一个 glyph 的纹理
        var texRef = new Object() { GpuTextureView textureView; };
        preparedText.visit(new Font.GlyphVisitor() {
            @Override public void acceptRenderable(TextRenderable r) {
                if (texRef.textureView == null) texRef.textureView = r.textureView();
            }
        });
        if (texRef.textureView == null) return;

        // 用 TextureSetup 设置纹理 + lightmap（同 Skyblocker）
        var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        var textureSetup = TextureSetup.singleTextureWithLightmap(texRef.textureView, sampler);

        var binding = pipeline.getVertexFormatBinding(0);
        if (binding == null) return;
        var draw = VERTEX_BUFFER.appendDraw(binding, pipeline.getPrimitiveTopology());

        // 渲染 glyph
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

        VERTEX_BUFFER.upload();

        var mainTarget = client.gameRenderer.mainRenderTarget();
        var colorView = mainTarget.getColorTextureView();
        if (colorView == null) return;

        // 同 Skyblocker Renderer: applyViewOffsetZLayering
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        RenderSystem.getProjectionType().applyLayeringTransform(modelViewStack, 1.0F);

        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "BabyzombieAddons WorldText",
                colorView, Optional.empty(),
                mainTarget.useDepth ? mainTarget.getDepthTextureView() : null,
                OptionalDouble.empty())) {

            RenderSystem.bindDefaultUniforms(renderPass);

            var execInfo = VERTEX_BUFFER.getExecuteInfo(draw);
            if (execInfo != null) {
                renderPass.setPipeline(pipeline);
                renderPass.setUniform("DynamicTransforms",
                        RenderSystem.getDynamicUniforms().writeTransform(
                                RenderSystem.getModelViewMatrixCopy(),
                                new org.joml.Vector4f(1, 1, 1, 1)));
                // 纹理绑定：同 Skyblocker
                if (textureSetup.texure0() != null)
                    renderPass.bindTexture("Sampler0", textureSetup.texure0(), textureSetup.sampler0());
                if (textureSetup.texure1() != null)
                    renderPass.bindTexture("Sampler1", textureSetup.texure1(), textureSetup.sampler1());
                renderPass.setVertexBuffer(0, execInfo.vertexBuffer().slice());
                renderPass.setIndexBuffer(execInfo.indexBuffer(), execInfo.indexType());
                renderPass.drawIndexed(execInfo.indexCount(), 1, execInfo.firstIndex(), execInfo.baseVertex(), 0);
            }
        }

        modelViewStack.popMatrix();
        VERTEX_BUFFER.endDraw();
        VERTEX_BUFFER.endFrame();
    }
}
