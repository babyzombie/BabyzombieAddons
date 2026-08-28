package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * 开局保护窗口：聊天栏出现 "Starting in 1 second." 到 Elle 提示登台
 * （"Head over to the main platform"）之间，吞掉服务器的关屏包
 * （见 ClientPacketListenerMixin），避免开局时正在查看的模组设置界面被关闭。
 */
public final class KuudraScreenProtector {
    private KuudraScreenProtector() {}

    /** 兜底超时：结束消息未出现时（异常情况）自动解除保护 */
    private static final long MAX_WINDOW_MS = 10_000;

    private static boolean active;
    private static long startMs;

    public static void init() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            String text = ChatUtils.stripColor(message.getString());
            if (text.equals("Starting in 1 second.")
                    && HypixelLocationTracker.getInstance().isInKuudra()) {
                active = true;
                startMs = System.currentTimeMillis();
            } else if (KuudraChatLines.isHeadToPlatform(text)) {
                active = false;
            }
            return true;
        });

        // 兜底：结束消息一直没出现时自动解除，避免保护残留到下一局
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (active && System.currentTimeMillis() - startMs > MAX_WINDOW_MS) {
                active = false;
            }
        });

        // 离开副本时立即解除
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> active = false);
    }

    /** 保护窗口是否生效（含配置开关判断，关闭开关时永远不生效） */
    public static boolean isActive() {
        return active && ModConfigManager.get().kuudra.phase1.protectLocalScreenOnStart;
    }
}
