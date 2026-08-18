package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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

    @Unique
    private static void markIfNeeded(Object submitNode) {
        EntityRenderState state = DepthTestSubmitTracker.CURRENT_ENTITY_STATE.get();
        // outlineColor == 0 的 submit 不会生成 outline，也不需要标记为深度测试；
        // 选择性发光时未选中的身体/槽位正是靠这个条件排除的。
        if (state != null
                && state.outlineColor != 0
                && state.getDataOrDefault(GlowController.NEEDS_DEPTH_TEST, false)) {
            DepthTestSubmitTracker.mark(submitNode);
        }
    }
}
