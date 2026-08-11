package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/// 修复 26.2 原版 offsetToFullyIncludeCameraCube 的死循环:
/// 原版循环"cube 不完全在视锥内就沿视锥方向后退",当视锥远平面
/// 短于相机 cube(8 格)的对角线时(第二相机低视距 16~18 格),
/// cube 在"进入视锥"和"穿出视锥末端"之间没有完全在内的位置,
/// 循环永不退出,主线程死循环(未响应)。
/// 重写为带迭代上限的版本:正常场景(视锥够长)行为与原版一致,
/// 极端场景 128 次(512 格)后退后直接结束,不再死锁。
@Mixin(Frustum.class)
public class FrustumOffsetLimitMixin {

    @Inject(method = "offsetToFullyIncludeCameraCube", at = @At("HEAD"), cancellable = true)
    private void babyzombieaddons$safeOffsetToFullyIncludeCameraCube(int cubeSize, CallbackInfoReturnable<Frustum> cir) {
        Frustum self = (Frustum) (Object) this;
        var intersection = ((FrustumAccessor) self).getIntersection();
        double camX = self.getCamX();
        double camY = self.getCamY();
        double camZ = self.getCamZ();
        double camX1 = Math.floor(camX / (double) cubeSize) * cubeSize;
        double camY1 = Math.floor(camY / (double) cubeSize) * cubeSize;
        double camZ1 = Math.floor(camZ / (double) cubeSize) * cubeSize;
        double camX2 = Math.ceil(camX / (double) cubeSize) * cubeSize;
        double camY2 = Math.ceil(camY / (double) cubeSize) * cubeSize;
        double camZ2 = Math.ceil(camZ / (double) cubeSize) * cubeSize;
        int guard = 0;
        while (intersection.intersectAab((float) (camX1 - self.getCamX()), (float) (camY1 - self.getCamY()),
                (float) (camZ1 - self.getCamZ()), (float) (camX2 - self.getCamX()),
                (float) (camY2 - self.getCamY()), (float) (camZ2 - self.getCamZ())) != -2
                && guard++ < 128) {
            self.offset(-4.0F);
        }
        cir.setReturnValue(self);
    }
}
