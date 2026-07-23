package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Util;
import top.babyzombie.addons.mixin.render.OutlineBufferSourceAccessor;

/**
 * 深度测试发光的独立渲染器。
 * 创建自己的 OutlineBufferSource 调用链 + 独立深度纹理，
 * 深度测试实体绕行到此 buffer，渲染时覆盖输出深度纹理实现遮挡。
 */
public final class DepthTestGlowRenderer {
    private static DepthTestGlowRenderer INSTANCE;

    private final Minecraft minecraft;
    private final OutlineBufferSource bufferSource;
    private GpuTexture depthTexture;
    private GpuTextureView depthTextureView;
    private boolean rendering;

    private DepthTestGlowRenderer() {
        this.minecraft = Minecraft.getInstance();
        this.bufferSource = Util.make(new OutlineBufferSource(), s ->
            ((OutlineBufferSourceAccessor) s).setOutlineBufferSource(
                new GlowBufferSource(new ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE))));
    }

    public static DepthTestGlowRenderer getInstance() {
        if (INSTANCE == null) INSTANCE = new DepthTestGlowRenderer();
        return INSTANCE;
    }

    public OutlineBufferSource getBufferSource() { return bufferSource; }

    public boolean isRendering() { return rendering; }

    public void updateDepth() {
        var main = minecraft.getMainRenderTarget();
        if (main == null) return;
        tryUpdateTexture();
        if (main.getDepthTexture() != null) {
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                main.getDepthTexture(), depthTexture,
                0, 0, 0, 0, 0,
                depthTexture.getWidth(0), depthTexture.getHeight(0)
            );
        }
    }

    private void begin() {
        rendering = true;
        RenderSystem.outputDepthTextureOverride = depthTextureView;
    }

    private void end() {
        rendering = false;
        RenderSystem.outputDepthTextureOverride = null;
    }

    /** 刷新所有累积的深度测试发光几何体 */
    public void endBatch() {
        begin();
        bufferSource.endOutlineBatch();
        end();
    }

    private void tryUpdateTexture() {
        int w = minecraft.getWindow().getWidth();
        int h = minecraft.getWindow().getHeight();
        if (depthTexture != null && depthTexture.getWidth(0) == w && depthTexture.getHeight(0) == h) return;

        if (depthTexture != null) { depthTexture.close(); depthTextureView.close(); }
        GpuDevice device = RenderSystem.getDevice();
        depthTexture = device.createTexture(
            () -> "Babyzombie Depth Glow Tex",
            GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
            TextureFormat.DEPTH32, w, h, 1, 1);
        depthTextureView = device.createTextureView(depthTexture);
    }

    /** 自定义 BufferSource，在 getBuffer/endBatch 时覆盖输出深度纹理 */
    private class GlowBufferSource extends MultiBufferSource.BufferSource {
        GlowBufferSource(ByteBufferBuilder buf) {
            super(buf, Object2ObjectSortedMaps.emptyMap());
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            if (startedBuilders.get(type) != null && !type.canConsolidateConsecutiveGeometry()) {
                begin();
                var vc = super.getBuffer(type);
                end();
                return vc;
            }
            return super.getBuffer(type);
        }

        @Override
        public void endBatch(RenderType type) {
            begin();
            super.endBatch(type);
            end();
        }
    }
}
