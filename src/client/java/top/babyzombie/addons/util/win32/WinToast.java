package top.babyzombie.addons.util.win32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.babyzombie.addons.module.misc.MinimizeToTrayModule;

import javax.imageio.ImageIO;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Windows 系统通知(纯 Java SystemTray 实现):
 * <p>
 * Win10/11 上 AWT 托盘通知会以系统通知样式弹出并进入通知中心,不需要外部进程、
 * 编码命令或计划任务。点击通知/托盘图标触发「恢复游戏窗口」。
 * 托盘图标懒添加:首次发送时加入托盘,短暂展示后自动移除,不常驻多余图标。
 * 非 Windows、无系统托盘或发送失败时静默降级为日志,不影响游戏。
 */
public final class WinToast {

    private static final Logger LOGGER = LoggerFactory.getLogger("BabyzombieAddons/WinToast");

    private static final boolean IS_WINDOWS =
        System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    /** 图标展示后自动移除的时长(毫秒) */
    private static final long ICON_LINGER_MS = 10_000;

    /** 相同内容 5 秒内重复发送会被合并 */
    private static final long COALESCE_MS = 5_000;

    private static final Object SEND_LOCK = new Object();
    private static String lastToastKey;
    private static long lastToastTime;
    private static volatile boolean notSupportedLogged;

    /** 托盘图标状态(EDT 访问,锁保护) */
    private static TrayIcon trayIcon;
    private static boolean trayIconAdded;
    private static final Object TRAY_LOCK = new Object();

    /** 定时移除托盘图标的调度器 */
    private static final ScheduledExecutorService REMOVER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "BZA-ToastIconRemover");
        t.setDaemon(true);
        return t;
    });

    /** 备用图标提供者(由游戏层注入,兼容 dev 环境 classpath 资源缺失) */
    private static volatile java.util.function.Supplier<InputStream> modIconProvider;

    private WinToast() {}

    /** 当前系统是否支持(仅 Windows 且有系统托盘、非 headless)。 */
    public static boolean isSupported() {
        return IS_WINDOWS && SystemTray.isSupported() && !GraphicsEnvironment.isHeadless();
    }

    /** 游戏启动时调用:预先关闭 headless(与托盘模块一致,必须在任何 AWT 使用前)。 */
    public static void init() {
        System.setProperty("java.awt.headless", "false");
    }

    /** 游戏退出时调用:移除托盘图标(幂等)。 */
    public static void shutdown() {
        EventQueue.invokeLater(WinToast::removeTrayIcon);
    }

    /** 注入备用图标提供者(游戏层调用;取不到返回 null)。 */
    public static void setModIconProvider(java.util.function.Supplier<InputStream> provider) {
        modIconProvider = provider;
    }

    /**
     * 发送一条系统通知(异步,不阻塞调用线程)。
     * 相同内容 5 秒内重复发送会被合并;失败仅记日志。
     */
    public static void send(String title, String message) {
        if (!isSupported()) {
            if (!notSupportedLogged) {
                notSupportedLogged = true;
                LOGGER.warn("System notification is only supported on Windows with a system tray, ignored");
            }
            return;
        }
        synchronized (SEND_LOCK) {
            long now = System.currentTimeMillis();
            String key = title + '\u0000' + message;
            if (key.equals(lastToastKey) && now - lastToastTime < COALESCE_MS) {
                LOGGER.debug("Coalesced duplicate system notification: {}", title);
                return;
            }
            lastToastKey = key;
            lastToastTime = now;
        }
        EventQueue.invokeLater(() -> {
            try {
                ensureIconAdded();
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
                REMOVER.schedule(WinToast::requestIconRemoval, ICON_LINGER_MS, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                LOGGER.warn("Failed to send system notification", e);
            }
        });
    }

    /** 首次发送时把托盘图标加入系统托盘(EDT)。 */
    private static void ensureIconAdded() throws Exception {
        synchronized (TRAY_LOCK) {
            if (trayIconAdded) {
                return;
            }
            if (trayIcon == null) {
                trayIcon = new TrayIcon(loadIcon(), "BabyzombieAddons");
                trayIcon.setImageAutoSize(true);
                // 点击通知/托盘图标 → 恢复游戏窗口
                trayIcon.addActionListener(e -> MinimizeToTrayModule.requestRestoreFromSystem());
            }
            SystemTray.getSystemTray().add(trayIcon);
            trayIconAdded = true;
        }
    }

    private static void requestIconRemoval() {
        EventQueue.invokeLater(WinToast::removeTrayIcon);
    }

    /** 移除托盘图标(幂等;EDT)。 */
    private static void removeTrayIcon() {
        synchronized (TRAY_LOCK) {
            if (trayIconAdded && trayIcon != null) {
                try {
                    SystemTray.getSystemTray().remove(trayIcon);
                } catch (Exception e) {
                    LOGGER.debug("Failed to remove tray icon", e);
                }
                trayIconAdded = false;
            }
        }
    }

    /** 图标:mod 自带 icon.png;获取失败时返回空白占位图。 */
    private static Image loadIcon() {
        try (InputStream in = modIconStream()) {
            if (in != null) {
                BufferedImage img = ImageIO.read(in);
                if (img != null) {
                    return img;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to load mod icon for tray notification", e);
        }
        return new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
    }

    private static InputStream modIconStream() {
        InputStream in = WinToast.class.getResourceAsStream("/assets/babyzombieaddons/icon.png");
        if (in == null) {
            java.util.function.Supplier<InputStream> provider = modIconProvider;
            if (provider != null) {
                in = provider.get();
            }
        }
        return in;
    }
}