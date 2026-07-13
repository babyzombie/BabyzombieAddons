package top.babyzombie.addons.module.chat;

import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.mixin.chat.ChatScreenAccessor;

public final class ContainerChatModule {

    private ContainerChatModule() {}

    public static void init() {
        ContainerClickEvents.BEFORE_MOUSE_CLICK.register((screen, slot, event) -> {
            if (!ModConfigManager.get().general.chat.chatInContainer) return false;
            if (ContainerChatHelper.isBlocklistedContainer(screen)) return false;
            if ((event.modifiers() & GLFW.GLFW_MOD_ALT) == 0) return false;
            if (slot == null || !slot.hasItem()) return false;

            var stack = slot.getItem();

            // 聊天未打开 → 切换收藏状态（Skyblocker > Firmament > 自维护）
            if (!ContainerChatHelper.isActive()) {
                ItemProtectBridge.toggle(stack);
                return true;
            }

            // 聊天已打开 → 分享物品名
            String itemName = stack.getHoverName().getString();
            if (stack.getCount() > 1) itemName += " x" + stack.getCount();

            var chatScreen = ContainerChatHelper.getOverlay();
            if (chatScreen != null) {
                ((ChatScreenAccessor) chatScreen).getInput().insertText(itemName + " ");
                return true;
            }
            return false;
        });
    }
}
