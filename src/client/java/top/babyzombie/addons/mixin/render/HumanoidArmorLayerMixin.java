package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.SelectiveGlowHelper;

/**
 * 选择性发光：盔甲层按 EquipmentSlot 临时打开/关闭 outlineColor。
 */
@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin {

    @Inject(method = "renderArmorPiece", at = @At("HEAD"))
    private void bza$applySelectiveArmorSlot(CallbackInfo ci,
            @Local(argsOnly = true) EquipmentSlot slot,
            @Local(argsOnly = true) HumanoidRenderState state) {
        SelectiveGlowHelper.applySlot(state, slot);
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"))
    private void bza$restoreSelectiveArmorSlot(CallbackInfo ci,
            @Local(argsOnly = true) HumanoidRenderState state) {
        SelectiveGlowHelper.restore(state);
    }
}
