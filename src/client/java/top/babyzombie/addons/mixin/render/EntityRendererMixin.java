package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.GlowController;

/**
 * 在 extractRenderState 中设置 EntityRenderState.outlineColor / 深度测试标记，
 * 使 GlowController 追踪的实体进入发光渲染管线。
 * 深度测试发光由 SubmitNodeCollectionMixin 代为提交自定义 outline（被墙挡）。
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(Entity entity, EntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (GlowController.shouldGlow(entity)) {
            int color = ARGB.opaque(GlowController.getGlowColor(entity));
            if (GlowController.isDepthTestEnabled(entity)) {
                // 不设 outlineColor：阻止原版 outline（无深度测试），由 SubmitNodeCollectionMixin 代为提交自定义 outline
                state.setData(GlowController.NEEDS_DEPTH_TEST, true);
                state.setData(GlowController.DEPTH_GLOW_COLOR, color);
            } else {
                state.outlineColor = color;
            }
        }
    }
}
