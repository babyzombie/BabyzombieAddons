package top.babyzombie.addons.module.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.mixin.ChatScreenAccessor;

public final class ContainerChatModule {

    private ContainerChatModule() {}

    public static void init() {
        ContainerClickEvents.BEFORE_MOUSE_CLICK.register((screen, slot, button) -> {
            if (!ModConfigManager.get().general.chatInContainer) return false;
            if (ContainerChatHelper.isBlocklistedContainer(screen)) return false;
            if (slot == null || !slot.hasItem()) return false;

            long window = Minecraft.getInstance().getWindow().handle();
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) != GLFW.GLFW_PRESS) return false;

            if (!ContainerChatHelper.isActive()) {
                var cs = new ChatScreen("", false);
                ContainerChatHelper.activate(screen, cs);
            }

            var chatScreen = ContainerChatHelper.getOverlay();
            if (chatScreen != null) {
                // TODO: 未来做详细物品信息分享（SkyBlock ID、附魔、属性等）
                var stack = slot.getItem();
                String itemName = stack.getHoverName().getString();
                if (stack.getCount() > 1) {
                    itemName += " x" + stack.getCount();
                }
                ((ChatScreenAccessor) chatScreen).getInput().insertText(itemName + " ");
                return true;
            }

            return false;
        });
    }
}
