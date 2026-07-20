package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;

/**
 * 管理深度测试发光所需的全局状态。
 * 配合 RenderPipelineMixin 控制 OUTLINE 管线的深度测试开关。
 */
public final class GlowRenderer {
    private GlowRenderer() {}

    public static final DepthStencilState DEPTH_TEST_STATE = DepthStencilState.DEFAULT;

    private static boolean depthTestActive;

    public static boolean isDepthTestActive() {
        return depthTestActive;
    }

    /** 在 render pass 创建前由 LevelRendererMixin 调用。 */
    public static void markDepthTestActive() {
        depthTestActive = true;
    }

    /** 在 executeOutline 后由 LevelRendererMixin 调用。 */
    public static void endDepthTestedOutline() {
        if (!depthTestActive) return;
        depthTestActive = false;
    }
}
