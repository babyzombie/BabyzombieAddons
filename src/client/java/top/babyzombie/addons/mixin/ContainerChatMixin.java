package top.babyzombie.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
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
            if (ContainerChatHelper.isInputFocused()) {
                ContainerChatHelper.getOverlay().keyPressed(event);
                cir.setReturnValue(true);
            } else if (opts.keyChat.matches(event) || opts.keyCommand.matches(event)) {
                ContainerChatHelper.setInputFocused(true);
                cir.setReturnValue(true);
            }
            return;
        }

        // 原版带文本输入框的容器（铁砧、创造搜索等）不激活聊天栏
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
                // 点击穿透到容器 → 失焦
                ContainerChatHelper.setInputFocused(false);
            }
        }
    }

}
