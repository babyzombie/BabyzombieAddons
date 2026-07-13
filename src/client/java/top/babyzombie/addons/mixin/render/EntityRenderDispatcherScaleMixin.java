package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.PlayerScaleState;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherScaleMixin {

    @Inject(method = "submit",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER))
    private void applyPlayerScale(EntityRenderState renderState, CameraRenderState camera,
                                   double x, double y, double z,
                                   PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                   CallbackInfo ci) {
        if (PlayerScaleState.LOCAL_PLAYER_STATES.remove(renderState)) {
            var cfg = ModConfigManager.get().general;
            if (cfg.playerScale.x != 1.0f || cfg.playerScale.y != 1.0f || cfg.playerScale.z != 1.0f) {
                poseStack.scale(cfg.playerScale.x, cfg.playerScale.y, cfg.playerScale.z);
            }
        }
    }
}
