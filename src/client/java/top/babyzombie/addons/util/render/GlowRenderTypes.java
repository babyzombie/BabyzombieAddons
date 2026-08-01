package top.babyzombie.addons.util.render;

import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.BiFunction;

/** 深度测试 outline 的 RenderType 工厂（实体 submitModel 与方块 BlockModelRenderState 共用）。 */
public final class GlowRenderTypes {
    private GlowRenderTypes() {}

    private static final BiFunction<Identifier, Boolean, RenderType> OUTLINE_DEPTH = Util.memoize(
        (tex, cull) -> RenderType.create("bz_outline_depth", RenderSetup.builder(cull
            ? DepthTestRenderPipelines.OUTLINE_CULL : DepthTestRenderPipelines.OUTLINE_NO_CULL)
            .withTexture("Sampler0", tex).setOutputTarget(OutputTarget.OUTLINE_TARGET)
            .setOutline(RenderSetup.OutlineProperty.IS_OUTLINE).createRenderSetup()));

    public static RenderType depthOutline(Identifier tex, boolean cull) {
        return OUTLINE_DEPTH.apply(tex, cull);
    }
}
