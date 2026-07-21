package top.babyzombie.addons.mixin.render;

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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.BabyzombieAddonsClient;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.AlphaSubmitNodeCollector;

@Mixin(ItemInHandRenderer.class)
public class RenderHandMixin {

    /// ---- 手臂/物品透明度 ----

    @ModifyVariable(method = "renderHandsWithItems", at = @At("HEAD"), argsOnly = true, name = "submitNodeCollector")
    private SubmitNodeCollector wrapHandCollector(SubmitNodeCollector submitNodeCollector) {
        float alpha = ModConfigManager.get().general.handRender.alpha;
        if (alpha >= 1.0f) return submitNodeCollector;
        return new AlphaSubmitNodeCollector(submitNodeCollector, alpha);
    }

    /// ---- disableAll ----

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void disableHandRender(CallbackInfo ci) {
        if (ModConfigManager.get().general.handRender.disableAll) {
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
        if (ModConfigManager.get().general.handRender.swapHands) {
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
        if (ModConfigManager.get().general.handRender.swapHands) {
            return InteractionHand.MAIN_HAND;
        }
        return hand;
    }

    @Redirect(method = "renderArmWithItem",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/InteractionHand;MAIN_HAND:Lnet/minecraft/world/InteractionHand;", opcode = Opcodes.GETSTATIC))
    private InteractionHand redirectMainHand() {
        if (ModConfigManager.get().general.handRender.swapHands) {
            return InteractionHand.OFF_HAND;
        }
        return InteractionHand.MAIN_HAND;
    }

    @Redirect(method = "renderArmWithItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;getMainArm()Lnet/minecraft/world/entity/HumanoidArm;"))
    private HumanoidArm redirectMainArm(AbstractClientPlayer player) {
        if (ModConfigManager.get().general.handRender.swapHands) {
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
        var cfg = ModConfigManager.get().general;
        float inv = hand == InteractionHand.MAIN_HAND
                ? (player.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F)
                : (player.getMainArm() == HumanoidArm.RIGHT ? -1.0F : 1.0F);
        poseStack.translate(inv * cfg.handRender.itemOffsetX, cfg.handRender.itemOffsetY, 0.0F);
        poseStack.scale(cfg.handRender.itemScale, cfg.handRender.itemScale, cfg.handRender.itemScale);
    }

    @Inject(method = "renderArmWithItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
                    ordinal = 1))
    private void applyItemScale1(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        var cfg = ModConfigManager.get().general;
        float inv = hand == InteractionHand.MAIN_HAND
                ? (player.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F)
                : (player.getMainArm() == HumanoidArm.RIGHT ? -1.0F : 1.0F);
        poseStack.translate(inv * cfg.handRender.itemOffsetX, cfg.handRender.itemOffsetY, 0.0F);
        poseStack.scale(cfg.handRender.itemScale, cfg.handRender.itemScale, cfg.handRender.itemScale);
    }

    @Inject(method = "renderPlayerArm",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
                    ordinal = 2,
                    shift = At.Shift.AFTER))
    private void applyArmScale(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, float inverseArmHeight, float attackValue, HumanoidArm arm, CallbackInfo ci) {
        float s = ModConfigManager.get().general.handRender.itemScale;
        float inv = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float t = 1.0F / s - 1.0F;
        poseStack.scale(s, s, s);
        poseStack.translate(inv * -0.2F * t, 0.5F * t, 0.0F);
    }
}
