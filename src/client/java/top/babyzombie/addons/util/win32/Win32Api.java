package top.babyzombie.addons.util.win32;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Memory;
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

    // ── CreateProcessW(静默拉起外部进程,如 PowerShell 弹系统通知) ──

    /** 新进程不创建控制台窗口(从 GUI 进程拉起命令行工具时不闪窗) */
    public static final int CREATE_NO_WINDOW = 0x08000000;

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

    /** STARTUPINFOW(CreateProcessW 用;全部字段置零,不重定向句柄) */
    public static class STARTUPINFOW extends Structure {
        public int cb;
        public Pointer lpReserved;
        public Pointer lpDesktop;
        public Pointer lpTitle;
        public int dwX;
        public int dwY;
        public int dwXSize;
        public int dwYSize;
        public int dwXCountChars;
        public int dwYCountChars;
        public int dwFillAttribute;
        public int dwFlags;
        public short wShowWindow;
        public short cbReserved2;
        public Pointer lpReserved2;
        public Pointer hStdInput;
        public Pointer hStdOutput;
        public Pointer hStdError;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("cb", "lpReserved", "lpDesktop", "lpTitle", "dwX", "dwY", "dwXSize", "dwYSize",
                "dwXCountChars", "dwYCountChars", "dwFillAttribute", "dwFlags", "wShowWindow", "cbReserved2",
                "lpReserved2", "hStdInput", "hStdOutput", "hStdError");
        }
    }

    /** PROCESS_INFORMATION(CreateProcessW 输出) */
    public static class PROCESS_INFORMATION extends Structure {
        public Pointer hProcess;
        public Pointer hThread;
        public int dwProcessId;
        public int dwThreadId;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("hProcess", "hThread", "dwProcessId", "dwThreadId");
        }
    }

    /** TOKEN_ELEVATION(GetTokenInformation TokenElevation 输出) */
    public static class TOKEN_ELEVATION extends Structure {
        public int TokenIsElevated;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("TokenIsElevated");
        }
    }

    // ── GDI(抓取游戏窗口图标 HICON → 像素) ──

    /** ICONINFO(GetIconInfo 输出;hbmMask/hbmColor 由调用方 DeleteObject 释放) */
    public static class ICONINFO extends Structure {
        public boolean fIcon;
        public int xHotspot;
        public int yHotspot;
        public Pointer hbmMask;
        public Pointer hbmColor;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("fIcon", "xHotspot", "yHotspot", "hbmMask", "hbmColor");
        }
    }

    /** BITMAP(GetObjectW 输出) */
    public static class BITMAP extends Structure {
        public int bmType;
        public int bmWidth;
        public int bmHeight;
        public int bmWidthBytes;
        public short bmPlanes;
        public short bmBitsPixel;
        public Pointer bmBits;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("bmType", "bmWidth", "bmHeight", "bmWidthBytes", "bmPlanes", "bmBitsPixel", "bmBits");
        }
    }

    public static class BITMAPINFOHEADER extends Structure {
        public int biSize;
        public int biWidth;
        public int biHeight;
        public short biPlanes;
        public short biBitCount;
        public int biCompression;
        public int biSizeImage;
        public int biXPelsPerMeter;
        public int biYPelsPerMeter;
        public int biClrUsed;
        public int biClrImportant;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("biSize", "biWidth", "biHeight", "biPlanes", "biBitCount", "biCompression",
                "biSizeImage", "biXPelsPerMeter", "biYPelsPerMeter", "biClrUsed", "biClrImportant");
        }
    }

    /** BITMAPINFO(GetDIBits 用;仅头部,无调色板) */
    public static class BITMAPINFO extends Structure {
        public BITMAPINFOHEADER bmiHeader = new BITMAPINFOHEADER();

        @Override
        protected List<String> getFieldOrder() {
            return List.of("bmiHeader");
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

        boolean GetIconInfo(Pointer hIcon, ICONINFO piconinfo);
    }

    // ── gdi32.dll(图标像素提取) ──

    public interface Gdi32 extends Library {
        Gdi32 INSTANCE = Native.load("gdi32", Gdi32.class);

        Pointer CreateCompatibleDC(Pointer hdc);

        boolean DeleteDC(Pointer hdc);

        boolean DeleteObject(Pointer ho);

        int GetObjectW(Pointer h, int c, Pointer pv);

        int GetDIBits(Pointer hdc, Pointer hbm, int uStartScan, int cScanLines, Pointer lpvBits, BITMAPINFO lpbi, int uUsage);
    }

    // ── kernel32.dll ──

    public interface Kernel32 extends Library {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

        Pointer GetModuleHandleW(WString lpModuleName);

        int GetCurrentThreadId();

        Pointer GetCurrentProcess();

        boolean CreateProcessW(WString lpApplicationName, Pointer lpCommandLine, Pointer lpProcessAttributes,
            Pointer lpThreadAttributes, boolean bInheritHandles, int dwCreationFlags, Pointer lpEnvironment,
            WString lpCurrentDirectory, STARTUPINFOW lpStartupInfo, PROCESS_INFORMATION lpProcessInformation);

        boolean CloseHandle(Pointer hObject);

        int WaitForSingleObject(Pointer hObject, int dwMilliseconds);

        int GetLastError();
    }

    // ── shell32.dll(托盘图标) ──

    public interface Shell32 extends Library {
        Shell32 INSTANCE = Native.load("shell32", Shell32.class);

        boolean Shell_NotifyIconW(int dwMessage, NOTIFYICONDATA lpData);
    }

    // ── advapi32.dll(提权检测) ──

    public interface Advapi32 extends Library {
        Advapi32 INSTANCE = Native.load("advapi32", Advapi32.class);

        boolean OpenProcessToken(Pointer processHandle, int desiredAccess, IntByReference tokenHandle);

        boolean GetTokenInformation(IntByReference tokenHandle, int tokenInformationClass,
            TOKEN_ELEVATION tokenInformation, int tokenInformationLength, IntByReference returnLength);
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

    /**
     * 以 CREATE_NO_WINDOW 静默拉起外部进程(不闪控制台窗口),不等待其退出。
     * 命令行为可写缓冲区(CreateProcessW 会改写命令行,不能传只读 WString)。
     * 失败时调用方可用 {@link Kernel32#GetLastError()} 读取错误码(两次 JNA 调用之间线程末次错误值不变)。
     */
    public static boolean createProcessHidden(String commandLine) {
        return createProcessHiddenAndWait(commandLine, 0);
    }

    /**
     * 以 CREATE_NO_WINDOW 静默拉起外部进程并等待其退出(最多 timeoutMs 毫秒,超时按成功处理)。
     * 用于必须先完成才能继续的场景(如发通知前的 AUMID 注册,防止第一次通知抢跑)。
     */
    public static boolean createProcessHiddenAndWait(String commandLine, int timeoutMs) {
        if (commandLine == null || commandLine.isEmpty()) {
            return false;
        }
        Memory cmd = new Memory((commandLine.length() + 1L) * 2L);
        cmd.setWideString(0, commandLine);
        STARTUPINFOW startupInfo = new STARTUPINFOW();
        startupInfo.cb = startupInfo.size();
        PROCESS_INFORMATION processInfo = new PROCESS_INFORMATION();
        boolean ok = Kernel32.INSTANCE.CreateProcessW(null, cmd, null, null, false,
            CREATE_NO_WINDOW, null, null, startupInfo, processInfo);
        if (!ok) {
            return false;
        }
        Kernel32.INSTANCE.WaitForSingleObject(processInfo.hProcess, timeoutMs);
        Kernel32.INSTANCE.CloseHandle(processInfo.hProcess);
        Kernel32.INSTANCE.CloseHandle(processInfo.hThread);
        return true;
    }
}
