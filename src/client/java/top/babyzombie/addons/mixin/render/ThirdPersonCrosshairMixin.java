package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.babyzombie.addons.config.ModConfigManager;

/**
 * Third-person crosshair: keep showing the crosshair in third-person view.
 *
 * <p>Uses {@code @WrapOperation} instead of {@code @Redirect} so it chains with
 * other mods targeting the same call (e.g. kic's vanilla {@code @Redirect} on
 * {@code CameraType.isFirstPerson()} in {@code Gui.extractCrosshair}): MixinExtras
 * applies wraps after vanilla redirectors, so the other mod's redirector always
 * applies first (its require check passes) and this wrapper nests around it.</p>
 */
@Mixin(Hud.class)
public abstract class ThirdPersonCrosshairMixin {

    @WrapOperation(method = "extractCrosshair", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
    private boolean bypassIsFirstPerson(CameraType cameraType, Operation<Boolean> original) {
        if (ModConfigManager.get().general.selfPlayerRender.showCrosshairInThirdPerson) {
            return true;
        }
        return original.call(cameraType);
    }
}