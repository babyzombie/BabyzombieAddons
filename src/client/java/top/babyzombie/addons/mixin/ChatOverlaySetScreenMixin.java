package top.babyzombie.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.chat.ContainerChatHelper;

@Mixin(Minecraft.class)
public class ChatOverlaySetScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        Minecraft self = (Minecraft) (Object) this;

        // 情况 1: overlay 活跃 + 被设为 null → ChatScreen 想关闭自己，清理 overlay
        if (ContainerChatHelper.isActive() && screen == null) {
            ContainerChatHelper.deactivate();
            ci.cancel();
            return;
        }

        // 情况 2: 容器是当前主屏幕 + 有人想换成 ChatScreen → 转为 overlay
        Screen current = self.screen;
        if (current instanceof AbstractContainerScreen<?> container
                && screen instanceof ChatScreen chatScreen
                && ModConfigManager.get().general.chatInContainer
                && !ContainerChatHelper.isBlocklistedContainer(container)) {
            ContainerChatHelper.activate(container, chatScreen);
            ci.cancel();
            return;
        }

        // 情况 3: overlay 活跃 + 有人想换成别的屏幕 → 先清理 overlay，允许正常替换
        if (ContainerChatHelper.isActive() && screen != null) {
            ContainerChatHelper.deactivate();
            // 不 cancel，让新屏幕正常替换（会走正常流程调用容器 removed/onClose）
        }
    }
}
