package top.babyzombie.addons.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.fishing.FishingCameraModule;

/// 在世界渲染完成之后(renderLevel 返回后)注入第二相机捕获。
@Mixin(GameRenderer.class)
public class GameRendererCameraCaptureMixin {

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
                    shift = At.Shift.AFTER))
    private void babyzombieaddons$afterRenderLevel(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        FishingCameraModule.capture(deltaTracker);
    }
}
