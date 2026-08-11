package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.SecondCameraRenderer;

/// 第二相机捕获期间,阻止第二个 renderLevel 污染主画面的区块状态(SC 26.2 同款):
/// - repositionCamera:第二相机位置可能触发 viewArea 重定位,主画面区块加载范围被挪走;
/// - SectionOcclusionGraph.update:第二相机会重写主画面的遮挡图,
///   导致主画面区块可见性/淡入状态错乱(闪烁、白线)。
@Mixin(LevelRenderer.class)
public class CaptureCancelSectionStateMixin {

    @Inject(method = "repositionCamera", at = @At("HEAD"), cancellable = true)
    private void babyzombieaddons$cancelRepositionCamera(CameraRenderState camera, CallbackInfo ci) {
        if (SecondCameraRenderer.capturing) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SectionOcclusionGraph;update(Lnet/minecraft/client/renderer/state/level/CameraRenderState;ILnet/minecraft/client/renderer/state/level/ChunkLoadingRenderState;)V"),
            cancellable = true)
    private void babyzombieaddons$cancelSectionOcclusionGraphUpdate(CallbackInfo ci) {
        if (SecondCameraRenderer.capturing) {
            ci.cancel();
        }
    }

    /// 阻止第二个 renderLevel 的区块编译:compileSections 会按第二相机状态把
    /// 浮标附近的区块标记重编译,编译中的区块在主画面渲染时闪黄。
    /// 浮标附近的区块在主 viewArea 内已编译,跳过编译不影响第二相机画面。
    @Inject(method = "compileSections", at = @At("HEAD"), cancellable = true)
    private void babyzombieaddons$cancelCompileSections(CameraRenderState cameraState, CallbackInfo ci) {
        if (SecondCameraRenderer.capturing) {
            ci.cancel();
        }
    }
}
