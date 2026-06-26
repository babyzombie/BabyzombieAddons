package top.babyzombie.addons.module.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import top.babyzombie.addons.mixin.chat.ChatScreenAccessor;

public final class ContainerChatHelper {

    private ContainerChatHelper() {}

    static ChatScreen overlay;
    static Screen host;
    private static GLFWCharCallbackI previousCharCallback;
    private static GLFWScrollCallbackI previousScrollCallback;
    private static GLFWMouseButtonCallbackI previousMouseButtonCallback;
    static boolean inputFocused;

    public static boolean isActive() { return overlay != null; }
    public static boolean isInputFocused() { return inputFocused; }
    public static ChatScreen getOverlay() { return overlay; }

    public static boolean isBlocklistedContainer(AbstractContainerScreen<?> screen) {
        String name = screen.getClass().getName();
        return name.contains("AnvilScreen") || name.contains("CreativeModeInventoryScreen");
    }

    public static boolean isBlocklistedScreen(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?> c) return isBlocklistedContainer(c);
        String name = screen.getClass().getName();
        return name.contains("SignEdit") || name.contains("BookEdit") || name.contains("CommandBlock");
    }

    public static void setInputFocused(boolean focused) {
        inputFocused = focused;
        if (overlay != null) {
            var input = ((ChatScreenAccessor) overlay).getInput();
            input.setFocused(focused);
            input.setEditable(focused);
        }
    }

    public static void activate(Screen screen, ChatScreen chatScreen) {
        host = screen;
        overlay = chatScreen;
        inputFocused = true;
        overlay.init(screen.width, screen.height);

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
        // ALT+左键 REI 物品：GLFW 层拦截，mods 参数直接给到，无需查询
        previousMouseButtonCallback = GLFW.glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (action == GLFW.GLFW_PRESS && button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                    && (mods & GLFW.GLFW_MOD_ALT) != 0) {
                String itemName = ReiHelper.getHoveredEntryName();
                if (itemName != null) {
                    ((ChatScreenAccessor) overlay).getInput().insertText(itemName + " ");
                    return;
                }
            }
            if (previousMouseButtonCallback != null) {
                previousMouseButtonCallback.invoke(w, button, action, mods);
            }
        });
    }

    public static void deactivate() {
        if (overlay != null) {
            long window = Minecraft.getInstance().getWindow().handle();
            var cb = GLFW.glfwSetCharCallback(window, previousCharCallback);
            var scb = GLFW.glfwSetScrollCallback(window, previousScrollCallback);
            var mcb = GLFW.glfwSetMouseButtonCallback(window, previousMouseButtonCallback);
            if (cb != null) cb.free();
            if (scb != null) scb.free();
            if (mcb != null) mcb.free();
            previousCharCallback = null;
            previousScrollCallback = null;
            previousMouseButtonCallback = null;

            overlay.removed();
            overlay = null;
            host = null;
            inputFocused = false;
        }
    }
}
