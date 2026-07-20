package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.AlphaSubmitNodeCollector;
import top.babyzombie.addons.util.PlayerScaleState;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherScaleMixin {

    /** 标记当前 submit 是否为本地玩家，供 {@link #wrapCollector} 使用 */
    @Unique
    private boolean renderingLocalPlayer;

    @Inject(method = "submit",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER))
    private void applyPlayerScale(EntityRenderState renderState, CameraRenderState camera,
                                   double x, double y, double z,
                                   PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                   CallbackInfo ci) {
        renderingLocalPlayer = PlayerScaleState.LOCAL_PLAYER_STATES.remove(renderState);
        if (renderingLocalPlayer) {
            var cfg = ModConfigManager.get().general.selfPlayerRender;
            if (cfg.x != 1.0f || cfg.y != 1.0f || cfg.z != 1.0f) {
                poseStack.scale(cfg.x, cfg.y, cfg.z);
            }
            PlayerScaleState.CURRENT_ALPHA.set(cfg.alpha);
        }
    }

    @ModifyArg(method = "submit",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit"
                           + "(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
                           + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                           + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                           + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"),
            index = 2)
    private SubmitNodeCollector wrapCollector(SubmitNodeCollector collector) {
        if (!renderingLocalPlayer) return collector;
        float alpha = PlayerScaleState.CURRENT_ALPHA.get();
        if (alpha >= 1.0f) return collector;
        return new AlphaSubmitNodeCollector(collector, alpha);
    }
}
