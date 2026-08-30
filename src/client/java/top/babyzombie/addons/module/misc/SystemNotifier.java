package top.babyzombie.addons.module.misc;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.win32.WinToast;

/**
 * 系统通知(Windows Toast)桥接:
 * <p>
 * 按配置的 {@link ModConfig.ToastNotifyWhen} 时机判断是否发送(窗口挂托盘 / 最小化 / 非焦点时,才值得打扰系统通知)。
 * 聊天监听:消息文本包含自己的玩家名即发系统通知,直接展示消息全文(不区分发送者)。
 */
public final class SystemNotifier {

    private SystemNotifier() {}

    /** 游戏启动时调用:注册聊天提名字监听。 */
    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) {
                return;
            }
            String text = message.getString();
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            String name = player.getGameProfile().name();
            if (name == null || name.isEmpty() || !text.contains(name)) {
                return;
            }
            sendMentionNotify(text, "");
        });
    }

    /** 弹出事件转发系统通知(时机按 toastNotifyWhen 配置)。 */
    public static void sendPopupNotify(String title, String body) {
        sendWhen(title, body, ModConfigManager.get().general.tray.toastNotifyWhen);
    }

    /** 聊天提名字转发系统通知(时机按 toastMentionWhen 配置)。 */
    public static void sendMentionNotify(String title, String body) {
        sendWhen(title, body, ModConfigManager.get().general.tray.toastMentionWhen);
    }

    private static void sendWhen(String title, String body, ModConfig.ToastNotifyWhen when) {
        if (!WinToast.isSupported()) {
            return;
        }
        boolean notify = switch (when) {
            case OFF -> false;
            case ALWAYS -> true;
            case TRAY_ONLY -> MinimizeToTrayModule.isTrayActive();
            case TRAY_OR_MINIMIZED -> MinimizeToTrayModule.isTrayActive() || MinimizeToTrayModule.isWindowMinimized();
            case NOT_FOCUSED -> !MinimizeToTrayModule.isWindowFocused();
        };
        if (notify) {
            WinToast.send(title, body);
        }
    }
}
