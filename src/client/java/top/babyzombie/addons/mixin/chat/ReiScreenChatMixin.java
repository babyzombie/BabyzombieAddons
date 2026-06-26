package top.babyzombie.addons.mixin.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.chat.ContainerChatHelper;
import top.babyzombie.addons.module.chat.ReiHelper;
import top.babyzombie.addons.util.StarIndicator;

/**
 * REI 配方查看页面等非容器屏幕的聊天 overlay 支持。
 */
@Mixin(Screen.class)
public abstract class ReiScreenChatMixin {

    // ========== keyPressed ==========

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof AbstractContainerScreen) return;
        if (!ModConfigManager.get().general.chatInContainer) return;
        var screen = (Screen) (Object) this;

        // ESC：走 onClose → setScreen(null) → ChatOverlaySetScreenMixin 路径（与发送消息相同）
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && ContainerChatHelper.isActive()) {
            ContainerChatHelper.getOverlay().onClose();
            cir.setReturnValue(true);
            return;
        }

        // 以下仅对 REI 配方页面生效
        if (!ReiHelper.isReiDisplayScreen(screen)) return;
        if (ContainerChatHelper.isBlocklistedScreen(screen)) return;

        var opts = Minecraft.getInstance().options;

        if (ContainerChatHelper.isActive()) {
            if (ContainerChatHelper.isInputFocused()) {
                ContainerChatHelper.getOverlay().keyPressed(event);
                cir.setReturnValue(true);
            } else if (opts.keyChat.matches(event) || opts.keyCommand.matches(event)) {
                ContainerChatHelper.setInputFocused(true);
                cir.setReturnValue(true);
            }
            return;
        }

        if (opts.keyChat.matches(event) || opts.keyCommand.matches(event)) {
            ContainerChatHelper.activate(screen, new ChatScreen("", false));
            cir.setReturnValue(true);
        }
    }

    // ========== afterMouseAction ==========

    @Inject(method = "afterMouseAction", at = @At("HEAD"))
    private void onAfterMouseAction(CallbackInfo ci) {
        if ((Object) this instanceof AbstractContainerScreen) return;

        if (ReiHelper.isReiDisplayScreen(this) && ContainerChatHelper.isActive()) {
            double mx = Minecraft.getInstance().mouseHandler.xpos();
            double my = Minecraft.getInstance().mouseHandler.ypos();
            var chatScreen = ContainerChatHelper.getOverlay();
            ContainerChatHelper.setInputFocused(chatScreen.isMouseOver(mx, my));
        }
    }

    // ========== extractRenderState ==========

    @Inject(method = "extractRenderState*", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor g, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if ((Object) this instanceof ChatScreen) return;
        if ((Object) this instanceof AbstractContainerScreen) return;
        if (ContainerChatHelper.isActive()) {
            ContainerChatHelper.getOverlay().extractRenderState(g, mouseX, mouseY, a);
        }
        if (GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS) {
            boolean sharing = ContainerChatHelper.isActive() && ModConfigManager.get().general.chatInContainer;
            StarIndicator.draw(g, mouseX, mouseY, sharing);
        }
    }
}
