package top.babyzombie.addons.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.event.AfterWorldRenderEvents;

/// 在世界渲染完成之后(renderLevel 返回后)派发 AfterWorldRenderEvents。
/// 注入点选 renderLevel 返回后而不是 Fabric 的 END_MAIN:
/// END_MAIN 在云/天气之前触发,期间重跑渲染管线会破坏 Iris 的 pipeline 状态
/// (主画面后续云/天气 pass NPE),renderLevel 完全返回后则无此问题。
@Mixin(GameRenderer.class)
public class GameRendererCameraCaptureMixin {

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
                    shift = At.Shift.AFTER))
    private void babyzombieaddons$afterRenderLevel(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        AfterWorldRenderEvents.fire(deltaTracker);
    }
}
