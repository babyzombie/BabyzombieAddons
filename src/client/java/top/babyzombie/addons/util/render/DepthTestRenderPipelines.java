package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class DepthTestRenderPipelines {
    private DepthTestRenderPipelines() {}
    public static final RenderPipeline OUTLINE_CULL = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.OUTLINE_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("babyzombieaddons", "outline_depth_cull"))
        .withDepthStencilState(DepthStencilState.DEFAULT).build());
    public static final RenderPipeline OUTLINE_NO_CULL = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.OUTLINE_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("babyzombieaddons", "outline_depth_no_cull"))
        .withDepthStencilState(DepthStencilState.DEFAULT).withCull(false).build());
}
