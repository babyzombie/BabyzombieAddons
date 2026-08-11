package top.babyzombie.addons.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.fishing.FishingCameraModule;

/// 在渲染提取结束后把钓鱼特写画面贴到屏幕(26.2:提取在 GameRenderer.extract)。
@Mixin(GameRenderer.class)
public class FishingCameraHudMixin {

    @Inject(method = "extract(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("RETURN"))
    private void babyzombieaddons$drawFishingFeed(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        FishingCameraModule.drawHud();
    }
}
