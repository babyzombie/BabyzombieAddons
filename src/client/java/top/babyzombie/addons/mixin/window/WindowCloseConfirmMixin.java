package top.babyzombie.addons.mixin.window;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.misc.ExitConfirmScreen;

/**
 * 窗口关闭二次确认。
 * <p>
 * 所有窗口关闭请求（点 × / Alt+F4 / 系统菜单，包括左上角图标）都汇聚到
 * 注册在 Window 上的 GLFW 关闭回调。这里接管 setWindowCloseCallback 的
 * 注册，把回调换成我们的处理：开关开启时取消关闭、改弹确认界面；
 * 关闭时执行原回调（保持原行为）。
 */
@Mixin(Window.class)
public abstract class WindowCloseConfirmMixin {

    @Inject(method = "setWindowCloseCallback", at = @At("HEAD"), cancellable = true)
    private void babyzombieAddons$ownWindowCloseCallback(Runnable task, CallbackInfo ci) {
        ci.cancel();
        long handle = ((Window) (Object) this).handle();
        GLFW.glfwSetWindowCloseCallback(handle, id -> {
            if (ModConfigManager.get().general.pauseScreen.confirmWindowClose) {
                ExitConfirmScreen.onWindowClose(Minecraft.getInstance(), id);
            } else if (task != null) {
                task.run();
            }
        });
    }
}
