package top.babyzombie.addons.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.fishing.FishingCameraModule;

/// 在 HUD 提取结束后把钓鱼特写画面贴到屏幕。
@Mixin(Gui.class)
public class FishingCameraHudMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void babyzombieaddons$drawFishingFeed(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        FishingCameraModule.drawHud(graphics);
    }
}
