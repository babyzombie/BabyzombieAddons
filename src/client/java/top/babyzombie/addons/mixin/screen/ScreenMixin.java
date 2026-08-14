package top.babyzombie.addons.mixin.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.kuudra.ChestCounter;
import top.babyzombie.addons.module.misc.AutoReconnectHelper;
import top.babyzombie.addons.util.gui.overlay.GuiOverlayManager;

@Mixin(net.minecraft.client.gui.screens.Screen.class)
public class ScreenMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (!isDisconnectedScreen(this)) return;
        int remaining = AutoReconnectHelper.tickCountdown();
        if (remaining == 0) {
            AutoReconnectHelper.reconnect();
        }
    }

    @Inject(method = "extractRenderState*", at = @At("RETURN"))
    private void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        ChestCounter.renderOnScreen(graphics, mouseX, mouseY);
        GuiOverlayManager.onRender((Screen) (Object) this, graphics, mouseX, mouseY, a);

        if (!isDisconnectedScreen(this)) return;
        int remaining = AutoReconnectHelper.getCountdownRemaining();
        if (remaining <= 0) return;

        var mc = Minecraft.getInstance();
        var font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int x = sw / 2;

        var countdown = Component.translatable("babyzombieaddons.reconnect.countdown", remaining)
                .withStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.GOLD));
        graphics.centeredText(font, countdown, x, sh - 60, 0xFFFFAA00);

        graphics.centeredText(font,
                Component.literal(AutoReconnectHelper.getLastServerIp()).withColor(0xFF888888),
                x, sh - 38, 0xFF888888);

        if (AutoReconnectHelper.getRetryCount() > 0) {
            var retry = Component.translatable("babyzombieaddons.reconnect.attempt",
                    AutoReconnectHelper.getRetryCount() + 1)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.centeredText(font, retry, x, sh - 49, 0xFFAAAAAA);
        }
    }

    // =====================================================================
    // mouseClicked / mouseReleased / mouseDragged 的注入已迁移至
    // BabyzombieAddonsClient.onInitializeClient() 中的 Fabric Screen API
    // 全局注册（ScreenEvents.AFTER_INIT + ScreenMouseEvents.allow/beforeXxx）。
    //
    // MC 26.1.2 中 Screen 类本身不重写 ContainerEventHandler 接口的三个
    // mouse default 方法，直接 @Mixin(Screen.class) 注入这三个方法会失败：
    // "could not find any targets matching 'mouseClicked' in Screen"。
    // 尝试 Mixin 到 ContainerEventHandler 接口也被 sponge-mixin 0.8.7 的
    // SubType$Standard 校验拒绝（@Mixin target type mismatch: ... is an interface）。
    // 因此使用 Fabric 官方 Screen Mouse Events API 作为最终方案。
    //
    // AbstractContainerScreen 子类重写了三方法，仍然由 ContainerClickMixin
    // 进行针对性注入（slotClicked / keyPressed 等），但全局的 GuiOverlayManager
    // 鼠标事件已统一走 Screen API，ContainerClickMixin 中对应片段已去除以防
    // AbstractContainerScreen 双重触发。分类 HUD 切换器（CHS）已内联到
    // HudEditScreen 自行处理，不再走此全局入口。
    // =====================================================================

    private static boolean isDisconnectedScreen(Object screen) {
        return screen instanceof DisconnectedScreen;
    }
}
