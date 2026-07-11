package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

/**
 * 管理深度测试发光所需的全局状态。
 * 配合 RenderPipelineMixin 控制 OUTLINE 管线的深度测试开关。
 */
public final class GlowRenderer {
    private GlowRenderer() {}

    public static final DepthStencilState DEPTH_TEST_STATE = DepthStencilState.DEFAULT;

    private static final Minecraft minecraft = Minecraft.getInstance();
    private static boolean depthTestActive;

    public static boolean isDepthTestActive() {
        return depthTestActive;
    }

    @Nullable
    public static GpuTexture getMainDepthTexture() {
        var main = minecraft.getMainRenderTarget();
        return main != null ? main.getDepthTexture() : null;
    }

    /** 在 render pass 创建前由 LevelRendererMixin 调用。 */
    public static void markDepthTestActive() {
        depthTestActive = true;
    }

    /** 在 endOutlineBatch 后由 OutlineBufferSourceMixin 调用。 */
    public static void endDepthTestedOutline() {
        if (!depthTestActive) return;
        depthTestActive = false;
    }
}
