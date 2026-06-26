package top.babyzombie.addons.mixin.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.kuudra.KuudraHPDisplay;

@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {

    @Inject(method = "extractRenderState*", at = @At("HEAD"))
    private void onRender(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        KuudraHPDisplay.onBossbarRender((BossHealthOverlay) (Object) this);
    }
}
