package top.babyzombie.addons.mixin.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.kuudra.KuudraScreenProtector;

/**
 * 服务器发关屏包（如 Kuudra 开局清理 GUI）时会无差别关闭当前屏幕，
 * 导致正在查看的模组设置界面被关掉。这里只让关屏包影响容器类屏幕：
 * - 真容器屏 / 背后映射着活跃容器 handler 的自定义屏（如 LoadoutDisplayScreen）→ 放行，正常关屏重置 handler
 * - 纯本地屏幕（模组设置、HUD 编辑等）→ 吞掉关屏包，屏幕保持
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleContainerClose", at = @At("HEAD"), cancellable = true)
    private void babyzombieAddons$protectLocalScreenOnContainerClose(
            ClientboundContainerClosePacket packet, CallbackInfo ci) {
        // 仅在 Kuudra 开局保护窗口内生效（含配置开关判断），窗口外保持原版行为
        if (!KuudraScreenProtector.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.gui.screen();
        if (screen == null || screen instanceof AbstractContainerScreen<?>) return;
        // 屏幕背后还挂着活跃容器 handler（自定义容器屏）：交给原版关屏，保证 handler 被重置
        if (mc.player != null && mc.player.containerMenu != mc.player.inventoryMenu) return;
        ci.cancel();
    }
}
