package top.babyzombie.addons.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.SecondCameraRenderer;

/// 第二相机捕获期间跳过区块编译调度(compileSections):
/// 只需要 applyFrustum 按第二相机视锥更新可见区块列表,
/// 编译调度会让第二相机视角的区块异步重建,主画面视距边缘的区块随之闪烁。
@Mixin(LevelRenderer.class)
public class LevelRendererUpdateMixin {

    @Inject(method = "update(Lnet/minecraft/client/Camera;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;compileSections(Lnet/minecraft/client/Camera;)V",
                    shift = At.Shift.BEFORE),
            cancellable = true)
    private void babyzombieaddons$skipCompileSections(Camera camera, CallbackInfo ci) {
        if (SecondCameraRenderer.capturing) {
            ci.cancel();
        }
    }
}
