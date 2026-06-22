package top.babyzombie.addons.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.misc.AutoReconnectHelper;

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
    private void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isDisconnectedScreen(this)) return;
        int remaining = AutoReconnectHelper.getCountdownRemaining();
        if (remaining <= 0) return;

        var mc = Minecraft.getInstance();
        var font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int x = sw / 2;

        // 倒计时 — 粗体 + 金色
        var countdown = Component.translatable("babyzombieaddons.reconnect.countdown", remaining)
                .withStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.GOLD));
        graphics.centeredText(font, countdown, x, sh - 60, 0xFFFFAA00);

        // IP
        graphics.centeredText(font,
                Component.literal(AutoReconnectHelper.getLastServerIp()).withColor(0xFF888888),
                x, sh - 38, 0xFF888888);

        // 重试次数 (>0)
        if (AutoReconnectHelper.getRetryCount() > 0) {
            var retry = Component.translatable("babyzombieaddons.reconnect.attempt",
                    AutoReconnectHelper.getRetryCount() + 1)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            graphics.centeredText(font, retry, x, sh - 49, 0xFFAAAAAA);
        }
    }

    private static boolean isDisconnectedScreen(Object screen) {
        return screen instanceof DisconnectedScreen;
    }
}
