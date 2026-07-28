package top.babyzombie.addons.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.HudSourceTracker;

/**
 * Brackets the HUD rendering pass in {@link Gui#extractRenderState(GuiGraphicsExtractor, DeltaTracker)}.
 * Sets tracking=true at HEAD so that draw-call interceptors can record bounding boxes,
 * then hides tracking and draws hover tooltip at RETURN.
 */
@Mixin(Gui.class)
public class GuiExtractRenderStateMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onExtractRenderStateHead(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        HudSourceTracker.startFrame(graphics);
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onExtractRenderStateReturn(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        HudSourceTracker.endFrame();
    }
}
