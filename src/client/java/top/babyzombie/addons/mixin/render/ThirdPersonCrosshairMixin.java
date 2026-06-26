package top.babyzombie.addons.mixin.render;

import net.minecraft.client.CameraType;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.babyzombie.addons.config.ModConfigManager;

@Mixin(Gui.class)
public abstract class ThirdPersonCrosshairMixin {

    @Redirect(method = "extractCrosshair", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
    private boolean bypassIsFirstPerson(CameraType cameraType) {
        if (ModConfigManager.get().general.showCrosshairInThirdPerson) {
            return true;
        }
        return cameraType.isFirstPerson();
    }
}
