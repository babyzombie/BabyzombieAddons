package top.babyzombie.addons.mixin.window;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.module.misc.WindowTitleModule;

@Mixin(Minecraft.class)
public class WindowTitleMixin {

    /// createTitle() 返回后：缓存最新原始标题，同时用自定义标题替换返回值
    @Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
    private void babyzombieAddons$captureAndOverrideTitle(CallbackInfoReturnable<String> cir) {
        WindowTitleModule.cachedOriginalTitle = cir.getReturnValue();
        String title = WindowTitleModule.buildTitle(WindowTitleModule.cachedOriginalTitle);
        if (title != null) {
            cir.setReturnValue(title);
        }
    }
}
