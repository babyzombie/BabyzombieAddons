package top.babyzombie.addons.module.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.babyzombie.addons.config.GeneralConfig.Tray;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.EntityRenderEvents;
import top.babyzombie.addons.event.ParticleRenderEvents;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.win32.Win32Api;
import top.babyzombie.addons.util.win32.WinToast;
import top.babyzombie.addons.util.win32.Win32Api.Kernel32;
import top.babyzombie.addons.util.win32.Win32Api.Shell32;
import top.babyzombie.addons.util.win32.Win32Api.User32;
import top.babyzombie.addons.util.win32.Win32Api.WndProc;

import com.sun.jna.CallbackReference;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.WString;

import java.awt.EventQueue;
import java.awt.SystemTray;
import java.lang.reflect.Proxy;
import java.util.Locale;

/**
 * 最小化到系统托盘(仅 Windows):
 * <ul>
 *   <li>点最小化按钮进托盘:拦截 {@code WM_SYSCOMMAND(SC_MINIMIZE)},改为隐藏窗口;</li>
 *   <li>右键最小化按钮进托盘:拦截 {@code WM_NCRBUTTONDOWN(HTMINBUTTON)}(Win10 系统覆盖了标题栏自绘,
 *       无法再加自绘按钮,改用右键手势,零绘制;系统在 DOWN 后接管右键,必须拦 DOWN);</li>
 *   <li>挂托盘静音 / 限帧 / 隐藏实体与粒子:通过事件与 mixin 拦截,不修改玩家设置,恢复时零还原;</li>
 *   <li>托盘图标用 Shell_NotifyIcon 自建(绕开 AWT TrayIcon:其右键事件不触发、菜单无法渲染中文);
 *       右键菜单为 Win32 原生菜单(恢复窗口 / 退出游戏),左键点击恢复;</li>
 *   <li>恢复/退出由 EDT 置位、主线程 tick 消费执行(MC 状态只在主线程改);恢复后托盘图标移除,再次挂托盘时重现。</li>
 * </ul>
 * 全部走 Win32 API,不调用 GLFW(为后续无 GLFW 的 MC 版本留余地)。
 * 非 Windows 或系统托盘不可用时整体静默禁用。
 */
public final class MinimizeToTrayModule {

    private static final Logger LOGGER = LoggerFactory.getLogger("BabyzombieAddons/MinimizeToTray");

    private static final boolean IS_WINDOWS =
        System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    /** 初始化重试上限(tick),窗口就绪前轮询 */
    private static final int MAX_INIT_ATTEMPTS = 600;

    /** 托盘图标延迟创建上限(tick):等 MC 翻译表加载完,右键菜单文字才能正确翻译 */
    private static final int MAX_TRAY_DELAY = 600;

    /** 托盘右键菜单项 id */
    private static final int MENU_RESTORE = 1;
    private static final int MENU_EXIT = 2;

    /** 托盘回调窗口类名(static 持有,类名内存须长期有效;窗口由专用消息循环线程创建) */
    private static final Pointer TRAY_WND_CLASS_PTR = createTrayWndClassName();
    private static final WString TRAY_WND_TITLE = new WString("");

    private static Pointer createTrayWndClassName() {
        Memory mem = new Memory(64);
        mem.setWideString(0, "BabyzombieAddonsTrayWnd");
        return mem;
    }

    private static int initAttempts;

    // ── 窗口钩子状态(仅主线程读写) ──

    private static Pointer hwnd;
    private static long oldWndProc;
    /** 强引用防止 JNA 回调被 GC */
    private static WndProc wndProcRef;
    private static boolean windowHooked;

    // ── 托盘状态 ──

    /** 系统托盘可用且初始化成功(一次性) */
    private static volatile boolean traySupported;
    /** 托盘图标已成功创建过一次(可被移除后复用) */
    private static volatile boolean trayIconReady;
    /** 托盘图标当前是否显示(仅挂托盘期间显示) */
    private static volatile boolean trayIconShown;
    private static int trayDelayTicks;

    // ── 专用消息循环线程(创建托盘回调窗口并泵送消息;TrackPopupMenu 必须在该线程调用) ──

    private static volatile Pointer trayWnd;
    private static Thread trayThread;
    private static volatile int trayThreadId;
    private static WndProc trayWndProcRef;
    private static boolean trayCreatePending;
    private static final java.util.concurrent.CountDownLatch TRAY_WND_READY = new java.util.concurrent.CountDownLatch(1);

    // ── 挂托盘状态(主线程) ──

    private static boolean trayActive;
    /** EDT 置位、主线程 tick 消费:恢复窗口请求 */
    private static volatile boolean restoreRequested;
    /** EDT 置位、主线程 tick 消费:退出游戏请求 */
    private static volatile boolean exitRequested;

    // ── 配置轮询:tooltip 同步 ──

    private static int tooltipTick;

    private MinimizeToTrayModule() {}

    public static void init() {
        if (!IS_WINDOWS) {
            return;
        }
        // 必须早于任何 AWT 初始化设置,否则 headless 判定可能已固化
        System.setProperty("java.awt.headless", "false");
        ClientTickEvents.END_CLIENT_TICK.register(MinimizeToTrayModule::onTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(MinimizeToTrayModule::onClientStopping);
        // 挂托盘时的拦截走事件(不改玩家设置),事件注册与窗口钩子无关,托盘不可用时自动不生效
        // 静音用 MODIFY 把声音替换为 0 音量(不触发 BEFORE_PLAY 的 cancel 路径,调用方拿到的永远是有效返回值)
        PlaySoundEvents.MODIFY.register(MinimizeToTrayModule::applyTrayMute);
        EntityRenderEvents.BEFORE_RENDER.register(entity -> isHideEntitiesInTrayActive());
        ParticleRenderEvents.BEFORE_CREATE.register((options, x, y, z, xa, ya, za) -> isHideParticlesInTrayActive());
    }

    // ── 生命周期 ──

    private static void onTick(Minecraft client) {
        if (!windowHooked) {
            tryInit(client);
            return;
        }
        // 启动自动最小化/挂托盘:窗口挂钩完成后即执行一次(主菜单阶段即可,与 AutoIS 同节奏)
        if (!startupBehaviorApplied) {
            applyStartupBehavior();
        }
        if (restoreRequested && trayActive) {
            restoreRequested = false;
            restoreFromTray();
        }
        // 启动自动挂托盘:等托盘图标就绪后再隐藏,避免隐藏后无法恢复
        if (startupHidePending && trayIconReady) {
            startupHidePending = false;
            hideToTray();
        }
        if (exitRequested) {
            exitRequested = false;
            LOGGER.debug("Exit requested from tray menu");
            client.stop();
            return;
        }
        // 延迟准备托盘:等翻译表加载完(主菜单/世界)且托盘线程就绪,但不在挂托盘前显示图标
        if (!trayIconReady && traySupported && !trayCreatePending && ++trayDelayTicks >= 40
            && client.getOverlay() == null && (client.screen != null || client.level != null)) {
            trayCreatePending = true;
            EventQueue.invokeLater(MinimizeToTrayModule::prepareTrayIcon);
        } else if (!trayIconReady && traySupported && trayDelayTicks >= MAX_TRAY_DELAY) {
            traySupported = false; // 迟迟未就绪,禁用拦截(防止隐藏后无图标可恢复)
        }
        // 低频同步托盘 tooltip = 当前窗口标题
        if (trayIconShown && (++tooltipTick % 100) == 0) {
            String title = Win32Api.getWindowTitle(hwnd);
            if (!title.isEmpty()) {
                EventQueue.invokeLater(() -> updateTrayTip(title));
            }
        }
    }

    private static void tryInit(Minecraft client) {
        if (initAttempts >= MAX_INIT_ATTEMPTS) {
            return;
        }
        initAttempts++;
        Pointer found = Win32Api.findMainWindow();
        if (found == null || Pointer.nativeValue(found) == 0) {
            return; // 窗口未就绪,下个 tick 重试
        }
        if (!SystemTray.isSupported()) {
            LOGGER.debug("System tray not supported, tray features disabled");
            windowHooked = true; // 不再重试,整体禁用
            return;
        }
        hwnd = found;
        oldWndProc = User32.INSTANCE.GetWindowLongPtrW(hwnd, Win32Api.GWL_WNDPROC);
        wndProcRef = MinimizeToTrayModule::onWndProc;
        long newProc = Pointer.nativeValue(CallbackReference.getFunctionPointer(wndProcRef));
        long previous = User32.INSTANCE.SetWindowLongPtrW(hwnd, Win32Api.GWL_WNDPROC, newProc);
        if (previous == 0) {
            LOGGER.error("SetWindowLongPtrW failed, tray features disabled");
            windowHooked = true;
            return;
        }
        windowHooked = true;
        traySupported = true;
        LOGGER.debug("Window hooked for tray features");
    }

    private static void onClientStopping(Minecraft client) {
        cleanup();
        WinToast.shutdown();
    }

    /** 还原窗口过程、停止托盘消息线程并移除托盘图标(幂等)。静音/限帧/隐藏实体走事件与 mixin,无需还原。 */
    private static void cleanup() {
        if (oldWndProc != 0 && hwnd != null && Pointer.nativeValue(hwnd) != 0) {
            try {
                User32.INSTANCE.SetWindowLongPtrW(hwnd, Win32Api.GWL_WNDPROC, oldWndProc);
            } catch (Throwable t) {
                LOGGER.warn("Failed to restore window proc", t);
            }
            oldWndProc = 0;
        }
        if (trayIconReady || trayIconShown) {
            trayIconReady = false;
            trayIconShown = false;
            EventQueue.invokeLater(MinimizeToTrayModule::hideTrayIcon);
        }
        // 停止托盘消息线程
        int tid = trayThreadId;
        if (tid != 0) {
            try {
                User32.INSTANCE.PostThreadMessageW(tid, Win32Api.WM_QUIT, 0, 0);
            } catch (Throwable t) {
                LOGGER.warn("Failed to stop tray thread", t);
            }
            trayThreadId = 0;
        }
    }

    // ── 窗口过程(主线程) ──

    private static long onWndProc(Pointer h, int msg, long wParam, long lParam) {
        try {
            switch (msg) {
                case Win32Api.WM_SYSCOMMAND -> {
                    if ((wParam & 0xFFF0L) == Win32Api.SC_MINIMIZE && isMinimizeEnabled()) {
                        hideToTray();
                        return 0;
                    }
                }
                case Win32Api.WM_NCRBUTTONDOWN -> {
                    // 右键标题栏最小化按钮 → 挂托盘。
                    // 必须拦 DOWN:系统在 DOWN 后接管右键(UP 不再发给窗口,直接弹系统菜单),
                    // 返回 0 阻止系统进入菜单模式。wParam 高 16 位带修饰键标志,只取低 16 位比较。
                    if ((wParam & 0xFFFF) == Win32Api.HTMINBUTTON && isButtonEnabled()) {
                        hideToTray();
                        return 0;
                    }
                }
                case Win32Api.WM_DESTROY -> {
                    long result = User32.INSTANCE.CallWindowProcW(oldWndProc, h, msg, wParam, lParam);
                    cleanup();
                    return result;
                }
            }
        } catch (Throwable t) {
            // 窗口过程绝不允许异常逃逸
            LOGGER.error("Error in window proc (msg={})", Integer.toHexString(msg), t);
        }
        return User32.INSTANCE.CallWindowProcW(oldWndProc, h, msg, wParam, lParam);
    }

    // ── 隐藏 / 恢复 ──

    private static void hideToTray() {
        if (trayActive || hwnd == null) {
            return;
        }
        trayActive = true;
        User32.INSTANCE.ShowWindow(hwnd, Win32Api.SW_HIDE);
        applyTraySilencing();
        if (trayIconReady && !trayIconShown) {
            EventQueue.invokeLater(MinimizeToTrayModule::createTrayIcon);
        }
        LOGGER.debug("Minimized to tray");
    }

    private static void restoreFromTray() {
        if (!trayActive || hwnd == null) {
            return;
        }
        trayActive = false;
        User32.INSTANCE.ShowWindow(hwnd, Win32Api.SW_RESTORE);
        User32.INSTANCE.SetForegroundWindow(hwnd);
        // 恢复后移除托盘图标,再次挂托盘时重现
        EventQueue.invokeLater(MinimizeToTrayModule::hideTrayIcon);
        LOGGER.debug("Restored from tray");
    }

    /**
     * 外部触发恢复窗口(点击系统通知 / 快捷方式):挂托盘时置位恢复请求,由主线程 tick 走完整恢复流程
     * (含移除托盘图标、解除静音等);未挂托盘时直接恢复并置前。可跨线程调用。
     */
    public static void requestRestoreFromSystem() {
        if (trayActive) {
            restoreRequested = true;
        } else if (hwnd != null && Pointer.nativeValue(hwnd) != 0) {
            User32.INSTANCE.ShowWindow(hwnd, Win32Api.SW_RESTORE);
            User32.INSTANCE.SetForegroundWindow(hwnd);
        }
    }

    /** 当前是否挂在托盘(供系统通知时机判断)。 */
    public static boolean isTrayActive() {
        return trayActive;
    }

    /** 启动自动行为是否已执行(每 JVM 会话至多一次)。 */
    private static volatile boolean startupBehaviorApplied;
    /** 启动自动挂托盘等待标志:等托盘图标就绪后由 onTick 执行隐藏。 */
    private static volatile boolean startupHidePending;

    /**
     * 启动自动最小化/挂托盘(窗口挂钩完成后由 onTick 调用一次,主菜单阶段即生效):
     * MINIMIZE = 最小化窗口;TRAY = 隐藏到托盘(等托盘图标就绪后执行,避免隐藏后无法恢复)。
     */
    public static void applyStartupBehavior() {
        if (!IS_WINDOWS || startupBehaviorApplied) {
            return;
        }
        startupBehaviorApplied = true;
        ModConfig.StartupMinimizeMode mode = ModConfigManager.get().general.tray.startupMinimizeMode;
        if (mode == ModConfig.StartupMinimizeMode.MINIMIZE && hwnd != null && Pointer.nativeValue(hwnd) != 0) {
            User32.INSTANCE.ShowWindow(hwnd, Win32Api.SW_MINIMIZE);
            LOGGER.debug("Window minimized on startup");
        } else if (mode == ModConfig.StartupMinimizeMode.TRAY) {
            startupHidePending = true;
        }
    }

    /** 窗口当前是否最小化(含挂托盘时隐藏);供系统通知时机判断。 */
    public static boolean isWindowMinimized() {
        return hwnd != null && Pointer.nativeValue(hwnd) != 0 && User32.INSTANCE.IsIconic(hwnd);
    }

    /** 窗口当前是否拥有焦点;供系统通知时机判断。 */
    public static boolean isWindowFocused() {
        if (hwnd == null || Pointer.nativeValue(hwnd) == 0) {
            return false;
        }
        Pointer fg = User32.INSTANCE.GetForegroundWindow();
        return fg != null && Pointer.nativeValue(fg) == Pointer.nativeValue(hwnd);
    }

    /** 进入托盘时一次性处理:停止当前声音 / 清空现有粒子。之后的新声音/粒子由事件持续拦截。 */
    private static void applyTraySilencing() {
        Tray tray = trayConfig();
        Minecraft client = Minecraft.getInstance();
        if (tray.muteInTray) {
            try {
                client.getSoundManager().stop();
            } catch (Throwable t) {
                LOGGER.warn("Failed to stop sounds while in tray", t);
            }
        }
        if (tray.hideParticlesInTray) {
            try {
                client.particleEngine.clearParticles();
            } catch (Throwable t) {
                LOGGER.warn("Failed to clear particles while in tray", t);
            }
        }
    }

    // ── 供事件 / mixin 查询的挂托盘状态(渲染线程与主线程相同,读配置安全) ──

    /** 挂托盘限帧:开启时返回帧率上限(1-60),否则返回 -1。FramerateLimitTrackerMixin 每帧调用。 */
    public static int getTrayFramerateLimit() {
        if (!trayActive) {
            return -1;
        }
        Tray cfg = trayConfig();
        if (!cfg.limitFpsInTray) {
            return -1;
        }
        return Math.clamp(cfg.trayFpsLimit, 1, 60);
    }

    /** 挂托盘静音:把声音替换为 0 音量的包装实例。不走 BEFORE_PLAY 取消,调用方拿到的永远是有效返回值。 */
    private static SoundInstance applyTrayMute(SoundInstance original) {
        return isMuteInTrayActive() ? muteSoundInstance(original) : original;
    }

    private static SoundInstance muteSoundInstance(SoundInstance original) {
        return (SoundInstance) Proxy.newProxyInstance(
            original.getClass().getClassLoader(),
            new Class<?>[] {SoundInstance.class},
            (proxy, method, args) -> method.getName().equals("getVolume") && method.getParameterCount() == 0
                ? 0.0F
                : method.invoke(original, args)
        );
    }

    private static boolean isMuteInTrayActive() {
        return trayActive && trayConfig().muteInTray;
    }

    private static boolean isHideEntitiesInTrayActive() {
        return trayActive && trayConfig().hideEntitiesInTray;
    }

    private static boolean isHideParticlesInTrayActive() {
        return trayActive && trayConfig().hideParticlesInTray;
    }

    // ── 托盘图标(Shell_NotifyIcon 自建;回调窗口由专用消息循环线程创建,右键菜单在该线程弹出) ──

    /** 只准备托盘能力(启动消息线程 + 创建回调窗口),不显示图标。 */
    private static void prepareTrayIcon() {
        trayCreatePending = false;
        ensureTrayReady();
    }

    /** 挂托盘时显示图标(准备 + NIM_ADD)。 */
    private static void createTrayIcon() {
        trayCreatePending = false;
        if (trayIconShown) {
            return;
        }
        if (!ensureTrayReady()) {
            return;
        }
        trayWndAdd();
    }

    /** 确保托盘消息线程与回调窗口就绪;返回 false 表示托盘不可用。 */
    private static boolean ensureTrayReady() {
        if (trayIconReady) {
            return true;
        }
        if (!SystemTray.isSupported()) {
            traySupported = false;
            return false;
        }
        ensureTrayThread();
        try {
            // 等待托盘线程创建好回调窗口(窗口创建很快,等待 2 秒兜底)
            if (!TRAY_WND_READY.await(2, java.util.concurrent.TimeUnit.SECONDS) || trayWnd == null) {
                LOGGER.error("Tray callback window not ready");
                traySupported = false;
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            traySupported = false;
            return false;
        }
        trayIconReady = true;
        return true;
    }

    private static void ensureTrayThread() {
        if (trayThread != null && trayThread.isAlive()) {
            return;
        }
        trayThread = new Thread(MinimizeToTrayModule::trayThreadMain, "BZA-Tray");
        trayThread.setDaemon(true);
        trayThread.start();
    }

    /** 专用线程:创建托盘回调窗口并运行消息循环。TrackPopupMenu 必须由有消息循环的线程调用。 */
    private static void trayThreadMain() {
        trayThreadId = Kernel32.INSTANCE.GetCurrentThreadId();
        try {
            trayWndProcRef = MinimizeToTrayModule::onTrayWndProc;
            long proc = Pointer.nativeValue(CallbackReference.getFunctionPointer(trayWndProcRef));
            Pointer hInstance = Kernel32.INSTANCE.GetModuleHandleW(null);
            Win32Api.WNDCLASSEX wc = new Win32Api.WNDCLASSEX();
            wc.cbSize = wc.size();
            wc.lpfnWndProc = new Pointer(proc);
            wc.hInstance = hInstance;
            wc.lpszClassName = TRAY_WND_CLASS_PTR;
            User32.INSTANCE.RegisterClassExW(wc);
            Pointer wnd = User32.INSTANCE.CreateWindowExW(0, TRAY_WND_CLASS_PTR, TRAY_WND_TITLE, Win32Api.WS_POPUP,
                0, 0, 0, 0, null, null, hInstance, null);
            trayWnd = wnd;
            TRAY_WND_READY.countDown();
            // 消息循环:泵送本线程窗口的所有消息
            Win32Api.MSG msg = new Win32Api.MSG();
            while (User32.INSTANCE.GetMessageW(msg, null, 0, 0) > 0) {
                User32.INSTANCE.DispatchMessageW(msg);
            }
        } catch (Throwable t) {
            LOGGER.error("Tray thread failed", t);
        } finally {
            if (trayWnd != null && Pointer.nativeValue(trayWnd) != 0) {
                User32.INSTANCE.DestroyWindow(trayWnd);
            }
            trayWnd = null;
            TRAY_WND_READY.countDown();
        }
    }

    /** 托盘回调窗口过程(专用消息线程)。收到 uCallbackMessage 时 lParam 低字为鼠标事件。 */
    private static long onTrayWndProc(Pointer h, int msg, long wParam, long lParam) {
        try {
            if (msg == Win32Api.WM_APP_TRAY_CALLBACK) {
                int event = (int) (lParam & 0xFFFF);
                if (event == Win32Api.WM_LBUTTONUP) {
                    restoreRequested = true;
                } else if (event == Win32Api.WM_RBUTTONUP || event == 0x007B /* WM_CONTEXTMENU */) {
                    showTrayMenu();
                }
                return 0;
            }
        } catch (Throwable t) {
            LOGGER.error("Error in tray window proc", t);
        }
        return User32.INSTANCE.DefWindowProcW(h, msg, wParam, lParam);
    }

    private static void trayWndAdd() {
        if (Shell32.INSTANCE.Shell_NotifyIconW(Win32Api.NIM_ADD, buildNid(true))) {
            trayIconShown = true;
            LOGGER.debug("Tray icon shown");
        } else {
            LOGGER.error("Shell_NotifyIcon NIM_ADD failed");
            traySupported = false;
            trayIconReady = false;
        }
    }

    private static void hideTrayIcon() {
        try {
            if (trayWnd != null && Pointer.nativeValue(trayWnd) != 0) {
                Shell32.INSTANCE.Shell_NotifyIconW(Win32Api.NIM_DELETE, buildNid(false));
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed to remove tray icon", t);
        }
        trayIconShown = false;
    }

    private static void updateTrayTip(String tip) {
        try {
            if (!trayIconShown || trayWnd == null) {
                return;
            }
            Shell32.INSTANCE.Shell_NotifyIconW(Win32Api.NIM_MODIFY, buildNid(true));
        } catch (Throwable t) {
            LOGGER.debug("Failed to update tray tip", t);
        }
    }

    /**
     * 托盘右键菜单(专用消息线程调用):Win32 原生菜单。
     * AWT PopupMenu 在 Windows 上无法渲染中文(显示为方框,JDK 已知问题);
     * TrackPopupMenu 需要调用线程拥有消息循环,故由托盘线程直接弹出,中文由系统菜单字体保证。
     */
    private static void showTrayMenu() {
        try {
            Pointer menu = User32.INSTANCE.CreatePopupMenu();
            if (menu == null || Pointer.nativeValue(menu) == 0) {
                return;
            }
            try {
                User32.INSTANCE.AppendMenuW(menu, Win32Api.MF_STRING, MENU_RESTORE,
                    new WString(ChatUtils.translate("babyzombieaddons.tray.restore")));
                User32.INSTANCE.AppendMenuW(menu, Win32Api.MF_STRING, MENU_EXIT,
                    new WString(ChatUtils.translate("babyzombieaddons.tray.exit")));
                Win32Api.POINT pt = new Win32Api.POINT();
                User32.INSTANCE.GetCursorPos(pt);
                long cmd = User32.INSTANCE.TrackPopupMenu(menu,
                    Win32Api.TPM_RIGHTBUTTON | Win32Api.TPM_RETURNCMD | Win32Api.TPM_NONOTIFY,
                    pt.x, pt.y, 0, trayWnd, null);
                if (cmd == MENU_RESTORE) {
                    restoreRequested = true;
                } else if (cmd == MENU_EXIT) {
                    exitRequested = true;
                }
            } finally {
                User32.INSTANCE.DestroyMenu(menu);
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to show tray menu", t);
        }
    }

    private static Win32Api.NOTIFYICONDATA buildNid(boolean withTip) {
        Win32Api.NOTIFYICONDATA nid = new Win32Api.NOTIFYICONDATA();
        nid.cbSize = nid.size();
        nid.hWnd = trayWnd; // 托盘线程创建的回调窗口
        nid.uID = 1;
        nid.uFlags = Win32Api.NIF_MESSAGE | Win32Api.NIF_ICON | (withTip ? Win32Api.NIF_TIP : 0);
        nid.uCallbackMessage = Win32Api.WM_APP_TRAY_CALLBACK;
        nid.hIcon = getTrayIconHandle();
        setTip(nid, Win32Api.getWindowTitle(hwnd));
        return nid;
    }

    /** 托盘图标直接用 MC 窗口的 HICON(与任务栏一致),取不到回退系统应用图标。 */
    private static Pointer getTrayIconHandle() {
        long hicon = User32.INSTANCE.SendMessageW(hwnd, Win32Api.WM_GETICON, Win32Api.ICON_BIG, 0);
        if (hicon == 0) {
            hicon = User32.INSTANCE.GetWindowLongPtrW(hwnd, Win32Api.GCLP_HICON);
        }
        if (hicon == 0) {
            return User32.INSTANCE.LoadIconW(null, new Pointer(Win32Api.IDI_APPLICATION));
        }
        return new Pointer(hicon);
    }

    private static void setTip(Win32Api.NOTIFYICONDATA nid, String tip) {
        if (tip == null || tip.isEmpty()) {
            return;
        }
        char[] buf = new char[128];
        int len = Math.min(tip.length(), 127);
        tip.getChars(0, len, buf, 0);
        nid.szTip = buf;
    }

    // ── 配置 ──

    private static Tray trayConfig() {
        return ModConfigManager.get().general.tray;
    }

    private static boolean isMinimizeEnabled() {
        return trayConfig().minimizeToTray && traySupported && trayIconReady;
    }

    private static boolean isButtonEnabled() {
        return trayConfig().trayButton && traySupported && trayIconReady;
    }
}
