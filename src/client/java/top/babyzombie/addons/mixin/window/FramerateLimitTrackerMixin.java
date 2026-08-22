package top.babyzombie.addons.mixin.window;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.module.misc.MinimizeToTrayModule;

/**
 * 挂托盘限帧:在 MC 帧率限制器返回前套一层托盘帧率上限。
 * 只影响"窗口隐藏到托盘且开启限帧"的时段,不修改玩家的帧率设置。
 */
@Mixin(FramerateLimitTracker.class)
public class FramerateLimitTrackerMixin {

    @Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
    private void babyzombieAddons$applyTrayFpsLimit(CallbackInfoReturnable<Integer> cir) {
        int trayLimit = MinimizeToTrayModule.getTrayFramerateLimit();
        if (trayLimit > 0) {
            cir.setReturnValue(Math.min(cir.getReturnValue(), trayLimit));
        }
    }
}
