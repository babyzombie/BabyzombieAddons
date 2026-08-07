package top.babyzombie.addons.module.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * 关闭游戏 / 断开连接 的二次确认界面。
 * <p>
 * 由两个 mixin 驱动：窗口关闭请求(mixin/window/WindowCloseConfirmMixin)
 * 和暂停页断开连接(mixin/screen/DisconnectConfirmMixin)。
 */
public final class ExitConfirmScreen extends Screen {

    public enum Action {
        /** 退出游戏（窗口关闭场景） */
        EXIT,
        /** 断开连接（暂停页断开按钮场景） */
        DISCONNECT
    }

    private final Screen parent;
    private final Action action;

    /// 断开连接确认后重新调用 disconnectFromWorld 时的递归放行标志
    private static boolean bypassingDisconnect;

    private ExitConfirmScreen(Screen parent, Action action) {
        super(action == Action.DISCONNECT
                ? Component.translatable("babyzombieaddons.exitConfirm.disconnectTitle")
                : Component.translatable("babyzombieaddons.exitConfirm.title"));
        this.parent = parent;
        this.action = action;
    }

    /// 窗口关闭请求（点 × / Alt+F4 / 系统菜单）：
    /// 确认界面已打开则直接退出；否则清除 GLFW shouldClose 标志取消关闭，再弹确认界面。
    /// LWJGL 3.4 在调用关闭回调前就会置位 shouldClose，必须显式清除才能取消关闭。
    public static void onWindowClose(Minecraft mc, long windowHandle) {
        if (mc.gui.screen() instanceof ExitConfirmScreen) {
            mc.stop();
        } else {
            GLFW.glfwSetWindowShouldClose(windowHandle, false);
            mc.gui.setScreen(new ExitConfirmScreen(mc.gui.screen(), Action.EXIT));
        }
    }

    /// 暂停页断开连接请求：弹确认界面
    public static void openDisconnectConfirm(Minecraft mc) {
        mc.gui.setScreen(new ExitConfirmScreen(mc.gui.screen(), Action.DISCONNECT));
    }

    /// 断开连接确认后的递归放行
    public static boolean isBypassingDisconnect() {
        return bypassingDisconnect;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 150;
        int gap = 20; // 两按钮中间空隙
        Component confirmLabel = this.action == Action.DISCONNECT
                ? CommonComponents.disconnectButtonLabel(this.minecraft.isLocalServer())
                : Component.translatable("menu.quit");
        this.addRenderableWidget(Button.builder(confirmLabel, button -> this.performAction())
                .bounds(centerX - buttonWidth - gap / 2, centerY - 10, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("menu.returnToGame"), button -> this.onClose())
                .bounds(centerX + gap / 2, centerY - 10, buttonWidth, 20).build());
    }

    private void performAction() {
        if (this.action == Action.DISCONNECT) {
            bypassingDisconnect = true;
            try {
                this.minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
            } finally {
                bypassingDisconnect = false;
            }
        } else {
            // 单人世界先走保存断开流程回标题，再停主循环（stop() 本身不保存世界）；
            // 多人直接退出，服务器侧处理
            if (this.minecraft.hasSingleplayerServer()) {
                bypassingDisconnect = true;
                try {
                    this.minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
                } finally {
                    bypassingDisconnect = false;
                }
            }
            this.minecraft.stop();
        }
    }

    @Override
    public void onClose() {
        if (this.parent != null) {
            this.minecraft.gui.setScreen(this.parent);
        } else {
            this.minecraft.gui.setScreen(null);
            this.minecraft.mouseHandler.grabMouse();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        gui.fill(0, 0, this.width, this.height, 0xC0101010);
        super.extractRenderState(gui, mouseX, mouseY, delta);
        // text() 的 boolean 是阴影而非居中，手动按字体宽度居中；金色标题
        int titleX = (this.width - this.font.width(this.title)) / 2;
        gui.text(this.font, this.title, titleX, this.height / 2 - 44, 0xFFFFFF55, true);
    }
}
