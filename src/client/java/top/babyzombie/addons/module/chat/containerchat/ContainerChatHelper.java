package top.babyzombie.addons.module.chat.containerchat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import top.babyzombie.addons.mixin.chat.ChatScreenAccessor;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ItemUtils;
import org.jetbrains.annotations.Nullable;

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

    /**
     * 按「发送物品格式」配置生成分享文本。容器物品、REI/RRV 悬停物品都走这里：
     * neuName 从 Skyblocker / custom_data 解析，拿不到（非 SkyBlock 物品）时回退为物品名。
     * 结果以 [物品] 方括号包裹，方便接收端识别边界。
     */
    public static String buildSendText(ItemStack stack) {
        String name = stack.getHoverName().getString();
        if (stack.getCount() > 1) name += " x" + stack.getCount();
        return "[" + applySendMode(ItemUtils.getNeuName(stack), name) + "]";
    }

    /**
     * 把物品分享文本插进聊天栏，并把焦点交回聊天输入框（分享完的下一步是继续编辑 / 发送）。
     * <p>两条分享路径的这次点击都被上层抢先消费掉，谁都不会替我们聚焦：
     * 容器物品走 {@code AbstractContainerScreen.mouseClicked}，其 HEAD 注入按「点没点聊天框」
     * 把焦点设成了 false；REI/RRV 物品在 GLFW 回调就被拦下，根本不进 mouseClicked /
     * afterMouseAction。所以插入文本后必须在这里补一次聚焦，
     * 否则焦点留在背包 / REI 页面上，接着打字没反应。
     */
    public static void insertShareText(ItemStack stack) {
        if (overlay == null) return;
        ((ChatScreenAccessor) overlay).getInput().insertText(buildSendText(stack) + " ");
        setInputFocused(true);
    }

    private static String applySendMode(@Nullable String skyblockId, String name) {
        var mode = ModConfigManager.get().general.chat.chatSendItemMode;
        return switch (mode) {
            case ID -> skyblockId != null ? skyblockId : name;
            case ID_NAME -> skyblockId != null ? skyblockId + " " + name : name;
            default -> name;
        };
    }

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
        // 已激活时直接忽略：点击聊天消息时 vanilla 会 setScreen(ChatScreen) 想重设主屏幕，
        // 重复 activate 会再次 init() 重建输入框（丢输入）并重复安装 GLFW 回调（回调链自指/断链）
        if (overlay != null) return;
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
                // 26.1 的 MouseHandler.xpos/ypos 是物理像素，ChatScreen.mouseScrolled 期望 GUI 缩放坐标
                double scale = Minecraft.getInstance().getWindow().getGuiScale();
                double mx = Minecraft.getInstance().mouseHandler.xpos() / scale;
                double my = Minecraft.getInstance().mouseHandler.ypos() / scale;
                overlay.mouseScrolled(mx, my, xOffset, yOffset);
            } else if (previousScrollCallback != null) {
                previousScrollCallback.invoke(w, xOffset, yOffset);
            }
        });
        // ALT+左键 REI / RRV 物品：GLFW 层拦截，mods 参数直接给到，无需查询
        previousMouseButtonCallback = GLFW.glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (action == InputConstants.PRESS && button == InputConstants.MOUSE_BUTTON_LEFT
                    && (mods & InputConstants.MOD_ALT) != 0) {
                ItemStack hovered = ReiHelper.getHoveredEntryStack();
                if (hovered == null) hovered = RrvHelper.getHoveredEntryStack();
                if (hovered != null) {
                    insertShareText(hovered);
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
