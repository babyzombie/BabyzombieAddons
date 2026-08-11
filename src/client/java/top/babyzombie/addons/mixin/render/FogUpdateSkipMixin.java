package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.SecondCameraRenderer;

/// 第二相机捕获期间跳过 fog buffer 更新:
/// 26.2 的 fog buffer 是共享环形缓冲(MappableRingBuffer),capture 重跑的
/// renderLevel 会把第二相机的雾写入,与主画面帧间轮换交错,
/// 主画面雾色被污染(斜向半透明黄块),子相机也读到错乱的雾。
/// 跳过更新后主画面 fog 保持主相机的,第二相机画面用旧 fog(无碍特写)。
@Mixin(FogRenderer.class)
public class FogUpdateSkipMixin {

    @Inject(method = "updateBuffer(Lnet/minecraft/client/renderer/fog/FogData;)V",
            at = @At("HEAD"), cancellable = true)
    private void babyzombieaddons$skipDuringCapture(FogData fogData, CallbackInfo ci) {
        if (SecondCameraRenderer.capturing) {
            ci.cancel();
        }
    }
}
