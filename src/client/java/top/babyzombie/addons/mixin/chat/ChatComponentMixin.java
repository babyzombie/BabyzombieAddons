package top.babyzombie.addons.mixin.chat;

import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.module.chat.ContainerChatHelper;

/**
 * 聊天组件通过 isChatFocused() 判断 ChatScreen 是否为主屏幕来决定显示多少行。
 * overlay 活跃时也返回 true，让聊天区域扩到满 20 行，同时消除消息双重渲染。
 */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @Inject(method = "isChatFocused", at = @At("HEAD"), cancellable = true)
    private void onIsChatFocused(CallbackInfoReturnable<Boolean> cir) {
        if (ContainerChatHelper.isActive()) {
            cir.setReturnValue(true);
        }
    }
}
