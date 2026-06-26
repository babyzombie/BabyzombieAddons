package top.babyzombie.addons.mixin.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;
import top.babyzombie.addons.module.chat.ItemProtectBridge;
import top.babyzombie.addons.util.StarIndicator;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.chat.ContainerChatHelper;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerChatMixin extends Screen {

    protected ContainerChatMixin(Component title) {
        super(title);
    }

    // ========== keyPressed ==========

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfigManager.get().general.chatInContainer) return;
        var opts = Minecraft.getInstance().options;

        if (ContainerChatHelper.isActive()) {
            // ESC 关 overlay，走 onClose → setScreen(null) → ChatOverlaySetScreenMixin 路径
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                ContainerChatHelper.getOverlay().onClose();
                cir.setReturnValue(true);
                return;
            }
            if (ContainerChatHelper.isInputFocused()) {
                ContainerChatHelper.getOverlay().keyPressed(event);
                cir.setReturnValue(true);
            } else if (opts.keyChat.matches(event) || opts.keyCommand.matches(event)) {
                ContainerChatHelper.setInputFocused(true);
                cir.setReturnValue(true);
            }
            return;
        }

        if (ContainerChatHelper.isBlocklistedContainer((AbstractContainerScreen<?>) (Object) this)) return;

        if (opts.keyChat.matches(event) || opts.keyCommand.matches(event)) {
            var cs = new ChatScreen("", false);
            ContainerChatHelper.activate((AbstractContainerScreen<?>) (Object) this, cs);
            cir.setReturnValue(true);
        }
    }

    // ========== extractRenderState ==========

    @Inject(method = "extractRenderState*", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor g, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (ContainerChatHelper.isActive()) {
            ContainerChatHelper.getOverlay().extractRenderState(g, mouseX, mouseY, a);
        }
        // ALT 按住时：保护→⭐ 常显；分享→箭头 仅开关打开时
        if (GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS) {
            boolean sharing = ContainerChatHelper.isActive() && ModConfigManager.get().general.chatInContainer;
            StarIndicator.draw(g, mouseX, mouseY, sharing);
        }
    }

    // ========== mouseClicked ==========

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (ContainerChatHelper.isActive()) {
            boolean handled = ContainerChatHelper.getOverlay().mouseClicked(event, doubleClick);
            if (handled) {
                cir.setReturnValue(true);
                ContainerChatHelper.setInputFocused(true);
            } else {
                ContainerChatHelper.setInputFocused(false);
            }
        }
    }

    // ========== extractSlot ==========

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void onExtractSlot(GuiGraphicsExtractor g, Slot slot, int unusedX, int unusedY, CallbackInfo ci) {
        if (!ItemProtectBridge.needsOwnProtection()) return;
        if (!slot.hasItem()) return;
        if (!ItemProtectBridge.isProtected(slot.getItem())) return;
        // 金色小三角标识
        g.fill(slot.x, slot.y, slot.x + 4, slot.y + 4, 0xFFFFD700);
    }
}
