package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.util.render.GlowRenderer;

/**
 * 控制 OUTLINE 管线的深度测试：
 * - 有实体请求深度测试发光 → 启用深度测试（DEFAULT），配合 entity_outline 中的主场景深度做遮挡剔除
 * - 无实体请求深度测试发光 → 禁用深度测试（null），保持原版 x-ray 行为
 */
@Mixin(RenderPipeline.class)
public class RenderPipelineMixin {

    @Inject(method = "getDepthStencilState", at = @At("HEAD"), cancellable = true)
    private void overrideDepthStencilForOutline(CallbackInfoReturnable<DepthStencilState> cir) {
        RenderPipeline self = (RenderPipeline) (Object) this;
        if (self != RenderPipelines.OUTLINE_CULL && self != RenderPipelines.OUTLINE_NO_CULL) return;

        if (GlowRenderer.isDepthTestActive()) {
            cir.setReturnValue(GlowRenderer.DEPTH_TEST_STATE);
        } else {
            cir.setReturnValue(null); // 强制禁用深度测试，恢复 x-ray
        }
    }
}
