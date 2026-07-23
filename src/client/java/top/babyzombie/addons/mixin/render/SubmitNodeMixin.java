package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.DepthTestSubmitTracker;
import top.babyzombie.addons.util.render.GlowController;

/**
 * 在 SubmitNodeCollection 创建 submit node 时，
 * 根据当前实体的 NEEDS_DEPTH_TEST 标记 submit node。
 */
@Mixin(SubmitNodeCollection.class)
public class SubmitNodeMixin {

    @Inject(method = "submitModel", at = @At("TAIL"))
    private void markModelSubmit(CallbackInfo ci,
            @Local(name = "modelSubmit") SubmitNodeStorage.ModelSubmit<?> modelSubmit) {
        markIfNeeded(modelSubmit);
    }

    @Inject(method = "submitItem", at = @At("RETURN"))
    private void markItemSubmit(CallbackInfo ci) {
        var self = (SubmitNodeCollection) (Object) this;
        var list = self.getItemSubmits();
        if (!list.isEmpty()) {
            markIfNeeded(list.getLast());
        }
    }

    private static void markIfNeeded(Object submitNode) {
        EntityRenderState state = DepthTestSubmitTracker.CURRENT_ENTITY_STATE.get();
        if (state != null && state.getDataOrDefault(GlowController.NEEDS_DEPTH_TEST, false)) {
            DepthTestSubmitTracker.mark(submitNode);
        }
    }
}
