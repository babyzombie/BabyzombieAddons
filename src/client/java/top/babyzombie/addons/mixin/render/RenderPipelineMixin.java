package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.util.render.DepthTestGlowRenderer;

/**
 * 控制 OUTLINE 管线的深度测试：
 * 仅在 DepthTestGlowRenderer 正在渲染深度测试发光时启用深度测试。
 * 其他时候不干预，原版/其他 mod 的深度设置正常生效。
 */
@Mixin(RenderPipeline.class)
public class RenderPipelineMixin {

    @Inject(method = "getDepthStencilState", at = @At("HEAD"), cancellable = true)
    private void overrideDepthStencilForOutline(CallbackInfoReturnable<DepthStencilState> cir) {
        RenderPipeline self = (RenderPipeline) (Object) this;
        if (self != RenderPipelines.OUTLINE_CULL && self != RenderPipelines.OUTLINE_NO_CULL) return;

        if (DepthTestGlowRenderer.getInstance().isRendering()) {
            cir.setReturnValue(DepthStencilState.DEFAULT);
        }
    }
}
