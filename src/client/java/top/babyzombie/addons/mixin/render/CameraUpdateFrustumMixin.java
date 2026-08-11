package top.babyzombie.addons.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.fishing.FishingCameraModule;
import top.babyzombie.addons.util.render.SecondCameraRenderer;

/// 第二相机捕获期间限制视锥远平面(depthFar):
/// update 内部按玩家视距计算 depthFar(很大),这里在 prepareCullFrustum 前改小,
/// 区块挑选只覆盖浮标附近,减少提交量;不碰区块加载(不产生闪烁)。
@Mixin(Camera.class)
public class CameraUpdateFrustumMixin {

    @Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;prepareCullFrustum(Lorg/joml/Matrix4fc;Lorg/joml/Matrix4f;Lnet/minecraft/world/phys/Vec3;)V",
                    shift = At.Shift.BEFORE))
    private void babyzombieaddons$limitDepthFar(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (SecondCameraRenderer.capturing) {
            ((CameraAccessor) this).setDepthFar(FishingCameraModule.secondCameraDepthFar());
        }
    }
}
