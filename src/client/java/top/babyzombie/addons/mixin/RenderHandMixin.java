package top.babyzombie.addons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.BabyzombieAddonsClient;
import top.babyzombie.addons.config.ModConfigManager;

@Mixin(ItemInHandRenderer.class)
public class RenderHandMixin {

    /// ---- disableAll ----

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void disableHandRender(CallbackInfo ci) {
        if (ModConfigManager.get().handRender.disableAll) {
            ci.cancel();
        }
    }

    /// ---- swapHands ----

    @ModifyArg(method = "renderHandsWithItems",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
                    ordinal = 0),
            index = 3)
    private InteractionHand swapMainHand(InteractionHand hand) {
        if (BabyzombieAddonsClient.handRenderSwapActive) {
            return InteractionHand.OFF_HAND;
        }
        return hand;
    }

    @ModifyArg(method = "renderHandsWithItems",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
                    ordinal = 1),
            index = 3)
    private InteractionHand swapOffHand(InteractionHand hand) {
        if (BabyzombieAddonsClient.handRenderSwapActive) {
            return InteractionHand.MAIN_HAND;
        }
        return hand;
    }

    @Redirect(method = "renderArmWithItem",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/InteractionHand;MAIN_HAND:Lnet/minecraft/world/InteractionHand;", opcode = Opcodes.GETSTATIC))
    private InteractionHand redirectMainHand() {
        if (BabyzombieAddonsClient.handRenderSwapActive) {
            return InteractionHand.OFF_HAND;
        }
        return InteractionHand.MAIN_HAND;
    }

    @Redirect(method = "renderArmWithItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;getMainArm()Lnet/minecraft/world/entity/HumanoidArm;"))
    private HumanoidArm redirectMainArm(AbstractClientPlayer player) {
        if (BabyzombieAddonsClient.handRenderSwapActive) {
            return player.getMainArm().getOpposite();
        }
        return player.getMainArm();
    }

    /// ---- item scale ----

    @Inject(method = "renderArmWithItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
                    ordinal = 0))
    private void applyItemScale0(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        float s = ModConfigManager.get().handRender.itemScale;
        poseStack.scale(s, s, s);
    }

    @Inject(method = "renderArmWithItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
                    ordinal = 1))
    private void applyItemScale1(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        float s = ModConfigManager.get().handRender.itemScale;
        poseStack.scale(s, s, s);
    }

    @Inject(method = "renderPlayerArm",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
                    ordinal = 2,
                    shift = At.Shift.AFTER))
    private void applyArmScale(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, float inverseArmHeight, float attackValue, HumanoidArm arm, CallbackInfo ci) {
        float s = ModConfigManager.get().handRender.itemScale;
        float inv = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float t = 1.0F / s - 1.0F;
        poseStack.scale(s, s, s);
        poseStack.translate(inv * -0.2F * t, 0.5F * t, 0.0F);
    }
}
