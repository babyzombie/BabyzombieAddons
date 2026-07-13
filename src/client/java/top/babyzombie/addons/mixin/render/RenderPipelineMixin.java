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
 * - 无实体请求深度测试发光 → 不干预，让原版/其他 mod 的深度设置正常生效
 */
@Mixin(RenderPipeline.class)
public class RenderPipelineMixin {

    @Inject(method = "getDepthStencilState", at = @At("HEAD"), cancellable = true)
    private void overrideDepthStencilForOutline(CallbackInfoReturnable<DepthStencilState> cir) {
        RenderPipeline self = (RenderPipeline) (Object) this;
        if (self != RenderPipelines.OUTLINE_CULL && self != RenderPipelines.OUTLINE_NO_CULL) return;

        // 仅在有实体需要深度测试发光时才干预管线
        // 不设置返回值时，原版/其他 mod 的深度设置正常生效
        if (GlowRenderer.isDepthTestActive()) {
            cir.setReturnValue(GlowRenderer.DEPTH_TEST_STATE);
        }
    }
}
