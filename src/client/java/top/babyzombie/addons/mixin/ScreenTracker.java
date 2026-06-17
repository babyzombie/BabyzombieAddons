package top.babyzombie.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.ScreenHelper;

@Mixin(Minecraft.class)
public class ScreenTracker {
    @Inject(method = "setScreenAndShow", at = @At("HEAD"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        ScreenHelper.setCurrent(screen);
    }
}
