package top.babyzombie.addons.mixin.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.config.ModConfigManager;

@Mixin(LivingEntity.class)
public class SwingDurationMixin {

    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
    private void overrideSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != Minecraft.getInstance().player) return;
        var cfg = ModConfigManager.get().general;
        if (!cfg.handRender.customSwingDuration) return;
        int duration = cfg.handRender.swingDurationTicks;
        if (duration <= 0) {
            cir.setReturnValue(Integer.MAX_VALUE);
        } else {
            cir.setReturnValue(duration);
        }
    }
}
