package top.babyzombie.addons.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.util.render.SecondCameraRenderer;

/// 第二相机捕获期间禁止发光实体提取(shouldShowEntityOutlines = false):
/// capture 重跑的 extract 会把发光实体状态提取出来,第二遍 renderLevel 的
/// outline pass 画到共享的 entityOutlineTarget,主画面后续 doEntityOutline
/// 用同一 target 输出错乱(实体出框/主画面黄闪)。
@Mixin(LevelExtractor.class)
public class ExtractEntityOutlineSkipMixin {

    @Inject(method = "shouldShowEntityOutlines", at = @At("HEAD"), cancellable = true)
    private void babyzombieaddons$skipDuringCapture(Camera camera, CallbackInfoReturnable<Boolean> cir) {
        if (SecondCameraRenderer.capturing) {
            cir.setReturnValue(false);
        }
    }
}
