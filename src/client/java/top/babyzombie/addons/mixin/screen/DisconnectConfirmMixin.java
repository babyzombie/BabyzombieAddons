package top.babyzombie.addons.mixin.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.misc.ExitConfirmScreen;

/**
 * 暂停页「断开连接」二次确认。
 * <p>
 * disconnectFromWorld 的唯一调用方是暂停页断开按钮（服务器踢出/断线
 * 走 ClientPacketListener 另一条链），拦截它不会误伤被动断开。
 */
@Mixin(Minecraft.class)
public abstract class DisconnectConfirmMixin {

    /// 开关开启时改为弹确认界面，确认后才真正断开
    @Inject(method = "disconnectFromWorld", at = @At("HEAD"), cancellable = true)
    private void babyzombieAddons$confirmDisconnect(Component message, CallbackInfo ci) {
        if (!ModConfigManager.get().general.pauseScreen.confirmDisconnect) return;
        if (ExitConfirmScreen.isBypassingDisconnect()) return;
        ci.cancel();
        ExitConfirmScreen.openDisconnectConfirm((Minecraft) (Object) this);
    }
}
