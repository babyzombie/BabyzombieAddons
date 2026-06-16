package top.babyzombie.addons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.config.ModConfigManager;

@Mixin(LivingEntityRenderer.class)
public abstract class PlayerScaleMixin {

    @Unique
    private boolean babyzombieaddons$isLocalPlayer;

    @Inject(method = "extractRenderState*", at = @At("HEAD"))
    private void onExtractState(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        this.babyzombieaddons$isLocalPlayer = (entity == Minecraft.getInstance().player);
    }

    @Inject(method = "submit*", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
            ordinal = 1, shift = At.Shift.AFTER))
    private void applyPlayerScale(LivingEntityRenderState state, PoseStack poseStack,
                                  SubmitNodeCollector collector, CameraRenderState cameraState,
                                  CallbackInfo ci) {
        if (this.babyzombieaddons$isLocalPlayer) {
            var cfg = ModConfigManager.get().general;
            if (cfg.playerScaleX != 1.0f || cfg.playerScaleY != 1.0f || cfg.playerScaleZ != 1.0f) {
                poseStack.scale(cfg.playerScaleX, cfg.playerScaleY, cfg.playerScaleZ);
            }
        }
    }
}
