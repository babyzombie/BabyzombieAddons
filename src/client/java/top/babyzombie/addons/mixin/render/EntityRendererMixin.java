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
 * 在 extractRenderState 中设置 EntityRenderState.outlineColor，
 * 使 GlowController 追踪的实体进入原版发光渲染管线。
 * 深度测试开关由 RenderPipelineMixin 全局控制。
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(Entity entity, EntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (GlowController.shouldGlow(entity)) {
            state.outlineColor = ARGB.opaque(GlowController.getGlowColor(entity));
            if (GlowController.isDepthTestEnabled(entity)) {
                state.setData(GlowController.NEEDS_DEPTH_TEST, true);
            }
        }
    }
}
