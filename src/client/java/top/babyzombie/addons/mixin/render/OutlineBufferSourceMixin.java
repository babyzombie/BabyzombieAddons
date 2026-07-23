package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.GlowRenderer;

@Mixin(LevelRenderer.class)
public class OutlineBufferSourceMixin {
    @Inject(
        method = "lambda$addMainPass$0",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeOutline()V",
            shift = At.Shift.AFTER))
    private void afterOutline(CallbackInfo ci) {
        GlowRenderer.endDepthTestedOutline();
    }
}
