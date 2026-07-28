package top.babyzombie.addons.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.HudSourceTracker;

/**
 * Brackets the HUD rendering pass in {@link Hud#extractRenderState(GuiGraphicsExtractor, DeltaTracker)}.
 * In MC 26.2+ the HUD was extracted from {@code Gui} into its own {@code Hud} class.
 */
@Mixin(Hud.class)
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
