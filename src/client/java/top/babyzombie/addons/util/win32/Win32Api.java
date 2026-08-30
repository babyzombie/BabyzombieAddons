package top.babyzombie.addons.util.win32;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;

import java.util.List;

/**
 * 仅 Windows 使用的 Win32 API 封装(纯 JNA 核心,不依赖 jna-platform)。
 * <p>
 * MC 26.1.2 运行时自带 jna 5.17.0,此处仅编译期引用、运行时由 MC 提供。
 * 刻意不碰 GLFW:LWJGL 可能在后续 MC 版本被移除,窗口句柄一律通过
 * {@link #findMainWindow()} 按进程 ID 枚举获取。
 */
public final class Win32Api {

    private Win32Api() {}

    // ── 窗口过程 / 窗口 ──

    public static final int GWL_WNDPROC = -4;
    public static final int GW_OWNER = 4;
    public static final int GCLP_HICON = -14;
    public static final int WM_SYSCOMMAND = 0x0112;
    public static final int WM_NCRBUTTONDOWN = 0x00A4;
    public static final int WM_DESTROY = 0x0002;
    public static final int WM_GETICON = 0x007F;

    public static final int SC_MINIMIZE = 0xF020;
    /** 标题栏最小化按钮的非客户区命中码(右键它进托盘) */
    public static final int HTMINBUTTON = 0x0008;

    public static final int SW_HIDE = 0;
    public static final int SW_MINIMIZE = 6;
    public static final int SW_RESTORE = 9;

    public static final int ICON_BIG = 1;

    // ── 原生菜单(托盘右键菜单;AWT PopupMenu 在 Windows 上无法渲染中文,改用 Win32 菜单) ──

    public static final int MF_STRING = 0x00000000;
    public static final int TPM_RETURNCMD = 0x00000100;
    public static final int TPM_RIGHTBUTTON = 0x00000002;
    public static final int TPM_NONOTIFY = 0x00000080;

    // ── Shell_NotifyIcon 托盘图标(绕开 AWT TrayIcon:其右键事件与中文菜单均有缺陷) ──

    public static final int NIM_ADD = 0x00000000;
    public static final int NIM_MODIFY = 0x00000001;
    public static final int NIM_DELETE = 0x00000002;
    public static final int NIF_MESSAGE = 0x00000001;
    public static final int NIF_ICON = 0x00000002;
    public static final int NIF_TIP = 0x00000004;
    /** 托盘回调窗口收到的自定义消息:lParam 低字为鼠标事件(WM_LBUTTONUP 等),wParam 为图标 id */
    public static final int WM_APP_TRAY_CALLBACK = 0x8002;
    public static final int WM_LBUTTONUP = 0x0202;
    public static final int WM_RBUTTONUP = 0x0205;

    public static final int WS_POPUP = 0x80000000;
    public static final int IDI_APPLICATION = 32512;
    public static final int WM_QUIT = 0x0012;
    /** 空消息:托盘右键菜单收起后向回调窗口投递,使菜单能被点击别处关闭(托盘菜单模式要求,见 showTrayMenu) */
    public static final int WM_NULL = 0x0000;

    // ── 结构 ──

    public static class RECT extends Structure {
        public int left;
        public int top;
        public int right;
        public int bottom;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("left", "top", "right", "bottom");
        }
    }

    public static class POINT extends Structure {
        public int x;
        public int y;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("x", "y");
        }
    }

    /** MSG(消息循环用) */
    public static class MSG extends Structure {
        public Pointer hwnd;
        public int message;
        public long wParam;
        public long lParam;
        public int time;
        public POINT pt = new POINT();

        @Override
        protected List<String> getFieldOrder() {
            return List.of("hwnd", "message", "wParam", "lParam", "time", "pt");
        }
    }

    public static class GUID extends Structure {
        public int data1;
        public short data2;
        public short data3;
        public byte[] data4 = new byte[8];

        @Override
        protected List<String> getFieldOrder() {
            return List.of("data1", "data2", "data3", "data4");
        }
    }

    /** NOTIFYICONDATAW(x64 布局,与 MSVC 对齐一致) */
    public static class NOTIFYICONDATA extends Structure {
        public int cbSize;
        public Pointer hWnd;
        public int uID;
        public int uFlags;
        public int uCallbackMessage;
        public Pointer hIcon;
        public char[] szTip = new char[128];
        public int dwState;
        public int dwStateMask;
        public char[] szInfo = new char[256];
        public int uTimeoutOrVersion;
        public char[] szInfoTitle = new char[64];
        public int dwInfoFlags;
        public GUID guidItem = new GUID();
        public Pointer hBalloonIcon;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("cbSize", "hWnd", "uID", "uFlags", "uCallbackMessage", "hIcon",
                "szTip", "dwState", "dwStateMask", "szInfo", "uTimeoutOrVersion", "szInfoTitle",
                "dwInfoFlags", "guidItem", "hBalloonIcon");
        }
    }

    /** WNDCLASSEXW(托盘回调窗口类) */
    public static class WNDCLASSEX extends Structure {
        public int cbSize;
        public int style;
        public Pointer lpfnWndProc;
        public int cbClsExtra;
        public int cbWndExtra;
        public Pointer hInstance;
        public Pointer hIcon;
        public Pointer hCursor;
        public Pointer hbrBackground;
        public Pointer lpszMenuName;
        public Pointer lpszClassName;
        public Pointer hIconSm;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("cbSize", "style", "lpfnWndProc", "cbClsExtra", "cbWndExtra", "hInstance",
                "hIcon", "hCursor", "hbrBackground", "lpszMenuName", "lpszClassName", "hIconSm");
        }
    }

    // ── 回调 ──

    /** 窗口过程。在创建窗口的线程(MC 主线程 / 托盘消息线程)执行。 */
    public interface WndProc extends Callback {
        long callback(Pointer hwnd, int uMsg, long wParam, long lParam);
    }

    public interface EnumWindowsProc extends Callback {
        boolean callback(Pointer hwnd, long lParam);
    }

    // ── user32.dll ──

    public interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class);

        boolean EnumWindows(EnumWindowsProc lpEnumFunc, long lParam);

        int GetWindowThreadProcessId(Pointer hWnd, IntByReference lpdwProcessId);

        Pointer GetWindow(Pointer hWnd, int uCmd);

        boolean IsWindowVisible(Pointer hWnd);

        long GetWindowLongPtrW(Pointer hWnd, int nIndex);

        long GetClassLongPtrW(Pointer hWnd, int nIndex);

        long SetWindowLongPtrW(Pointer hWnd, int nIndex, long dwNewLong);

        long CallWindowProcW(long lpPrevWndFunc, Pointer hWnd, int uMsg, long wParam, long lParam);

        boolean ShowWindow(Pointer hWnd, int nCmdShow);

        boolean SetForegroundWindow(Pointer hWnd);

        boolean IsIconic(Pointer hWnd);

        Pointer GetForegroundWindow();

        long SendMessageW(Pointer hWnd, int uMsg, long wParam, long lParam);

        boolean PostMessageW(Pointer hWnd, int uMsg, long wParam, long lParam);

        int GetWindowTextW(Pointer hWnd, char[] lpString, int nMaxCount);

        boolean GetCursorPos(POINT lpPoint);

        Pointer CreatePopupMenu();

        boolean AppendMenuW(Pointer hMenu, int uFlags, long uIDNewItem, WString lpNewItem);

        long TrackPopupMenu(Pointer hMenu, int uFlags, int x, int y, int nReserved, Pointer hWnd, RECT lprc);

        boolean DestroyMenu(Pointer hMenu);

        int RegisterClassExW(WNDCLASSEX lpWndClass);

        Pointer CreateWindowExW(int dwExStyle, Pointer lpClassName, WString lpWindowName, int dwStyle,
            int x, int y, int nWidth, int nHeight, Pointer hWndParent, Pointer hMenu, Pointer hInstance,
            Pointer lpParam);

        boolean DestroyWindow(Pointer hWnd);

        Pointer LoadIconW(Pointer hInstance, Pointer lpIconName);

        long DefWindowProcW(Pointer hWnd, int uMsg, long wParam, long lParam);

        boolean PostThreadMessageW(int idThread, int uMsg, long wParam, long lParam);

        int GetMessageW(MSG lpMsg, Pointer hWnd, int wMsgFilterMin, int wMsgFilterMax);

        boolean DispatchMessageW(MSG lpMsg);
    }

    // ── kernel32.dll ──

    public interface Kernel32 extends Library {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

        Pointer GetModuleHandleW(WString lpModuleName);

        int GetCurrentThreadId();
    }

    // ── shell32.dll(托盘图标) ──

    public interface Shell32 extends Library {
        Shell32 INSTANCE = Native.load("shell32", Shell32.class);

        boolean Shell_NotifyIconW(int dwMessage, NOTIFYICONDATA lpData);
    }

    // ── 工具方法 ──

    /** 按进程 ID 查找本进程的可见主窗口(不依赖窗口类名,兼容后续无 GLFW 的版本)。 */
    public static Pointer findMainWindow() {
        final Pointer[] found = {Pointer.NULL};
        final long pid = ProcessHandle.current().pid();
        User32.INSTANCE.EnumWindows((hwnd, lParam) -> {
            IntByReference windowPid = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, windowPid);
            if (windowPid.getValue() == pid
                && User32.INSTANCE.IsWindowVisible(hwnd)
                && User32.INSTANCE.GetWindow(hwnd, GW_OWNER) == null) {
                found[0] = hwnd;
                return false; // 停止枚举
            }
            return true;
        }, 0);
        return found[0];
    }

    /** 读取窗口当前标题(与任务栏显示一致)。 */
    public static String getWindowTitle(Pointer hwnd) {
        char[] buf = new char[512];
        int len = User32.INSTANCE.GetWindowTextW(hwnd, buf, buf.length);
        return len > 0 ? new String(buf, 0, len) : "";
    }
}
