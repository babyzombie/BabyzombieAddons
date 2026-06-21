package top.babyzombie.addons.module.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import top.babyzombie.addons.mixin.ChatScreenAccessor;

/**
 * 管理 ChatScreen overlay 的状态，在 Mixin 之间共享。
 * charTyped 无法通过 Mixin 注入（26.1.2 无 refMap 时继承方法混淆），
 * 改用 GLFW char callback 劫持字符事件。
 * 焦点由自身标志位跟踪，不依赖 Minecraft 焦点系统（跨 widget 树不可靠）。
 */
public final class ContainerChatHelper {

    private ContainerChatHelper() {}

    static ChatScreen overlay;
    static AbstractContainerScreen<?> host;
    private static GLFWCharCallbackI previousCharCallback;
    private static GLFWScrollCallbackI previousScrollCallback;
    static boolean inputFocused;

    public static boolean isActive() {
        return overlay != null;
    }

    public static boolean isInputFocused() {
        return inputFocused;
    }

    /**
     * 这些容器有内置文本输入（铁砧改名、创造搜索等），Minecraft 会强抢键盘，
     * 聊天 overlay 无法正常工作，直接黑名单不启用。
     */
    public static boolean isBlocklistedContainer(AbstractContainerScreen<?> screen) {
        String name = screen.getClass().getName();
        return name.contains("AnvilScreen") || name.contains("CreativeModeInventoryScreen");
    }

    public static void setInputFocused(boolean focused) {
        inputFocused = focused;
        if (overlay != null) {
            var input = ((ChatScreenAccessor) overlay).getInput();
            input.setFocused(focused);
            // setFocused 不一定能停下光标闪烁（Minecraft 内部 tick 可能无视），
            // 补 setEditable 彻底让输入框变灰
            input.setEditable(focused);
        }
    }

    public static ChatScreen getOverlay() {
        return overlay;
    }

    public static void activate(AbstractContainerScreen<?> container, ChatScreen chatScreen) {
        host = container;
        overlay = chatScreen;
        inputFocused = true;
        overlay.init(container.width, container.height);

        long window = Minecraft.getInstance().getWindow().handle();
        previousCharCallback = GLFW.glfwSetCharCallback(window, (w, codepoint) -> {
            if (inputFocused) {
                overlay.charTyped(new CharacterEvent(codepoint));
            } else if (previousCharCallback != null) {
                previousCharCallback.invoke(w, codepoint);
            }
        });
        previousScrollCallback = GLFW.glfwSetScrollCallback(window, (w, xOffset, yOffset) -> {
            if (overlay != null) {
                double mx = Minecraft.getInstance().mouseHandler.xpos();
                double my = Minecraft.getInstance().mouseHandler.ypos();
                overlay.mouseScrolled(mx, my, xOffset, yOffset);
            } else if (previousScrollCallback != null) {
                previousScrollCallback.invoke(w, xOffset, yOffset);
            }
        });
    }

    public static void deactivate() {
        if (overlay != null) {
            long window = Minecraft.getInstance().getWindow().handle();
            GLFW.glfwSetCharCallback(window, previousCharCallback);
            GLFW.glfwSetScrollCallback(window, previousScrollCallback);
            previousCharCallback = null;
            previousScrollCallback = null;

            overlay.removed();
            overlay = null;
            host = null;
            inputFocused = false;
        }
    }
}
