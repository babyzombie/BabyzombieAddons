package top.babyzombie.addons.mixin;

import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.misc.AutoReconnectHelper;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends net.minecraft.client.gui.screens.Screen {

    protected DisconnectedScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (AutoReconnectHelper.shouldStartCountdown()) {
            AutoReconnectHelper.startCountdown(AutoReconnectHelper.getDelay());
        }
    }
}
