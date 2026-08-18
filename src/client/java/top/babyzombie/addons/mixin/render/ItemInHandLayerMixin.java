package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.SelectiveGlowHelper;

/**
 * 选择性发光：手持物品按主手/副手临时打开/关闭 outlineColor。
 */
@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {

    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void bza$applySelectiveHandSlot(CallbackInfo ci,
            @Local(argsOnly = true) ArmedEntityRenderState state,
            @Local(argsOnly = true) HumanoidArm arm) {
        EquipmentSlot slot = arm == state.mainArm ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        SelectiveGlowHelper.applySlot(state, slot);
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"))
    private void bza$restoreSelectiveHandSlot(CallbackInfo ci,
            @Local(argsOnly = true) ArmedEntityRenderState state) {
        SelectiveGlowHelper.restore(state);
    }
}
