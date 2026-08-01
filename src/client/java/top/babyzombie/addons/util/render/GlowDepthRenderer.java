package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * 深度测试 outline 用独立深度纹理（照搬 Skyblocker GlowRenderer 方案）。
 * 主场景深度拷贝到此纹理，outline 渲染时把深度附件替换成它，
 * 绕开原版 entityOutlineTarget 深度的 clear/copyDepthFrom 干扰。
 */
public final class GlowDepthRenderer implements AutoCloseable {
    public static final GlowDepthRenderer INSTANCE = new GlowDepthRenderer();
    private @Nullable GpuTexture glowDepthTexture;
    private @Nullable GpuTextureView glowDepthTextureView;

    private GlowDepthRenderer() {}

    public GpuTextureView getGlowDepthTexture() {
        return Objects.requireNonNull(this.glowDepthTextureView);
    }

    /** 拷贝主渲染目标深度到独立纹理（在主 pass 场景深度就绪后调用）。 */
    public void updateGlowDepthTexDepth() {
        tryUpdateDepthTexture();
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
            Minecraft.getInstance().gameRenderer.mainRenderTarget().getDepthTexture(),
            this.glowDepthTexture, 0, 0, 0, 0, 0,
            this.glowDepthTexture.getWidth(0), this.glowDepthTexture.getHeight(0));
    }

    private void tryUpdateDepthTexture() {
        int neededWidth = Minecraft.getInstance().getWindow().getWidth();
        int neededHeight = Minecraft.getInstance().getWindow().getHeight();

        if (this.glowDepthTexture == null
            || this.glowDepthTexture.getWidth(0) != neededWidth
            || this.glowDepthTexture.getHeight(0) != neededHeight) {
            GpuDevice device = RenderSystem.getDevice();

            if (this.glowDepthTexture != null) {
                this.glowDepthTexture.close();
                this.glowDepthTextureView.close();
            }

            this.glowDepthTexture = device.createTexture(
                () -> "BabyzombieAddons Glow Depth",
                GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                GpuFormat.D32_FLOAT, neededWidth, neededHeight, 1, 1);
            this.glowDepthTextureView = device.createTextureView(this.glowDepthTexture);
        }
    }

    @Override
    public void close() {
        if (this.glowDepthTexture != null) {
            this.glowDepthTexture.close();
            this.glowDepthTextureView.close();
        }
    }
}
