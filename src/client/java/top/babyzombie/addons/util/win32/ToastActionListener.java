package top.babyzombie.addons.util.win32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.babyzombie.addons.module.misc.MinimizeToTrayModule;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * 系统通知 / 快捷方式的点击信号监听(仅 Windows):
 * <p>
 * 在 127.0.0.1 固定端口接收注册脚本中 PowerShell 发送的 UDP 信号包,触发「恢复游戏窗口」。
 * 只监听回环地址,Windows 防火墙对 loopback 不弹授权窗;绑定失败(端口被占)静默降级为日志。
 */
public final class ToastActionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("BabyzombieAddons/ToastAction");

    /** 与注册脚本中 PowerShell 发送端保持一致的端口 */
    public static final int PORT = 48236;

    /** 包头:'B','Z','A',0x01 */
    private static final byte[] MAGIC = {'B', 'Z', 'A', 0x01};

    /** 动作:1 = 恢复游戏窗口 */
    private static final byte ACTION_RESTORE = 0x01;

    private static volatile boolean started;

    private ToastActionListener() {}

    /** 启动回环 UDP 监听(幂等;仅 Windows,由 WinToast.init 调用)。 */
    public static void start() {
        if (started) {
            return;
        }
        started = true;
        Thread thread = new Thread(ToastActionListener::run, "BZA-ToastAction");
        thread.setDaemon(true);
        thread.start();
    }

    private static void run() {
        try (DatagramSocket socket = new DatagramSocket(PORT, InetAddress.getByName("127.0.0.1"))) {
            byte[] buffer = new byte[16];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                if (packet.getLength() >= MAGIC.length + 1 && matchesMagic(buffer)) {
                    byte action = buffer[MAGIC.length];
                    if (action == ACTION_RESTORE) {
                        MinimizeToTrayModule.requestRestoreFromSystem();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Toast action listener stopped: {}", e.toString());
        }
    }

    private static boolean matchesMagic(byte[] buffer) {
        for (int i = 0; i < MAGIC.length; i++) {
            if (buffer[i] != MAGIC[i]) {
                return false;
            }
        }
        return true;
    }
}
