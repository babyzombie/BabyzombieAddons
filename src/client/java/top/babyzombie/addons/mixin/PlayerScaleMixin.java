package top.babyzombie.addons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.config.ModConfigManager;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@Mixin(LivingEntityRenderer.class)
public abstract class PlayerScaleMixin {

    @Unique
    private static final Set<LivingEntityRenderState> LOCAL_PLAYER_STATES =
            Collections.newSetFromMap(new IdentityHashMap<>());

    @Inject(method = "extractRenderState*", at = @At("HEAD"))
    private void onExtractState(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
        if (entity == Minecraft.getInstance().player) {
            LOCAL_PLAYER_STATES.add(state);
        }
    }

    @Inject(method = "submit*", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
            ordinal = 1, shift = At.Shift.AFTER))
    private void applyPlayerScale(LivingEntityRenderState state, PoseStack poseStack,
                                  SubmitNodeCollector submitNodeCollector, CameraRenderState camera,
                                  CallbackInfo ci) {
        if (LOCAL_PLAYER_STATES.remove(state)) {
            var cfg = ModConfigManager.get().general;
            if (cfg.playerScaleX != 1.0f || cfg.playerScaleY != 1.0f || cfg.playerScaleZ != 1.0f) {
                poseStack.scale(cfg.playerScaleX, cfg.playerScaleY, cfg.playerScaleZ);
            }
        }
    }
}
