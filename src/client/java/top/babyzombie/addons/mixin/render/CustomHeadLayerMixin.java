package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.SelectiveGlowHelper;

/**
 * 选择性发光：头部物品/头颅按 HEAD 槽位临时打开/关闭 outlineColor。
 */
@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerMixin {

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("HEAD"))
    private void bza$applySelectiveHeadSlot(CallbackInfo ci,
            @Local(argsOnly = true) LivingEntityRenderState state) {
        SelectiveGlowHelper.applySlot(state, EquipmentSlot.HEAD);
    }

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("RETURN"))
    private void bza$restoreSelectiveHeadSlot(CallbackInfo ci,
            @Local(argsOnly = true) LivingEntityRenderState state) {
        SelectiveGlowHelper.restore(state);
    }
}
