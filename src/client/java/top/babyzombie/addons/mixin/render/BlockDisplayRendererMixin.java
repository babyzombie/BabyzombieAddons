package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;

/**
 * BlockDisplay 的 submitInner 不检查 isInvisible，身体和轮廓一起渲染。
 * 当实体隐身 + 发光时，改用 submitOnlyOutline，只渲染轮廓。
 */
@Mixin(DisplayRenderer.BlockDisplayRenderer.class)
public class BlockDisplayRendererMixin {

    @Inject(method = "submitInner*", at = @At("HEAD"), cancellable = true)
    private void onSubmitInner(BlockDisplayEntityRenderState state, PoseStack poseStack,
                               SubmitNodeCollector submitNodeCollector, int lightCoords,
                               float interpolationProgress, CallbackInfo ci) {
        if (state.isInvisible && state.outlineColor != 0) {
            state.blockModel.submitOnlyOutline(
                    poseStack, submitNodeCollector, lightCoords,
                    OverlayTexture.NO_OVERLAY, state.outlineColor
            );
            ci.cancel();
        }
    }
}
