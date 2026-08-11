package top.babyzombie.addons.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.SecondCameraRenderer;

/// 第二相机捕获期间跳过 setupFog:
/// 主画面的 fog 状态(含内部渐变)按玩家位置维护,capture 里按浮标位置重算会污染它,
/// 导致主视角水下雾闪烁;跳过后代用主画面的 fog(浮标离玩家近,视觉差异小)。
@Mixin(GameRenderer.class)
public class ExtractCameraFogMixin {

    @Inject(method = "extractCamera(Lnet/minecraft/client/DeltaTracker;FF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/fog/FogRenderer;setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/renderer/fog/FogData;",
                    shift = At.Shift.BEFORE),
            cancellable = true)
    private void babyzombieaddons$skipSetupFog(DeltaTracker deltaTracker, float worldPartialTicks,
                                               float cameraEntityPartialTicks, CallbackInfo ci) {
        if (SecondCameraRenderer.capturing) {
            ci.cancel();
        }
    }
}
