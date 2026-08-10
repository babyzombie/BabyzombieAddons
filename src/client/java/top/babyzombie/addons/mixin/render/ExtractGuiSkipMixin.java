package top.babyzombie.addons.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.fishing.FishingCameraModule;

/// 第二相机捕获期间跳过 GUI 提取:
/// capture 不需要 GUI 状态,提取会污染主画面的 GUI(准星不消失、物品栏重复渲染),
/// 跳过后主画面 GUI 状态保持主画面自己提取的结果。
@Mixin(GameRenderer.class)
public class ExtractGuiSkipMixin {

    @Inject(method = "extract(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V",
                    shift = At.Shift.BEFORE),
            cancellable = true)
    private void babyzombieaddons$skipGui(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        if (FishingCameraModule.capturing) {
            ci.cancel();
        }
    }
}
