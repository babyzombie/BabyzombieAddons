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
 * 在 extractRenderState 中设置 EntityRenderState.outlineColor / 选择性发光标记，
 * 使 GlowController 追踪的实体进入原版发光渲染管线。
 * 深度测试开关由 RenderPipelineMixin 全局控制。
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(Entity entity, EntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (GlowController.shouldGlow(entity)) {
            int color = ARGB.opaque(GlowController.getGlowColor(entity));
            state.setData(GlowController.GLOW_COLOR, color);

            if (GlowController.isSelectiveGlow(entity)) {
                // 选择性发光：先全局关掉 outline，由各部位 layer mixin 按需临时打开
                state.outlineColor = 0;
                state.setData(GlowController.SELECTIVE_SLOTS, GlowController.getGlowSlots(entity));
                if (GlowController.isDepthTestEnabled(entity)) {
                    state.setData(GlowController.NEEDS_DEPTH_TEST, true);
                }
            } else {
                state.outlineColor = color;
                if (GlowController.isDepthTestEnabled(entity)) {
                    state.setData(GlowController.NEEDS_DEPTH_TEST, true);
                }
            }
        }
    }
}
