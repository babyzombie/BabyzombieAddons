package top.babyzombie.addons.util.win32;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.babyzombie.addons.util.win32.Win32Api.Advapi32;
import top.babyzombie.addons.util.win32.Win32Api.Kernel32;
import top.babyzombie.addons.util.win32.Win32Api.TOKEN_ELEVATION;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Windows 通知中心系统通知(仅 Windows):
 * <p>
 * 通过系统自带的 Windows PowerShell 5.1(powershell.exe)加载 WinRT 类型弹出 Toast,
 * 不引入任何新依赖;进程用 {@link Win32Api#createProcessHidden} 以 CREATE_NO_WINDOW 拉起,无控制台闪现。
 * <p>
 * AUMID 注册(Win10 必需,实测 Win10 22H2 未注册时 Show() 不报错、也进平台历史,但弹窗与通知中心均不显示;
 * Win11 可不注册直接显示,注册亦无害):首次发送前通过 PowerShell 建开始菜单快捷方式并写入
 * AppUserModelID 属性,注册进程同步等待退出后再发通知,防止第一次抢跑。注册幂等,每 JVM 会话至多一次。
 * <p>
 * 通知左上角图标优先抓取游戏窗口图标(任务栏同款,经 HICON → PNG),失败回退 mod 自带 icon.png(appLogoOverride)。
 * <p>
 * 通用发送接口:先封装能力,具体触发事件(托盘期间的提醒等)后续再接入。
 * 非 Windows、注册失败或拉起失败时静默降级为日志,不影响游戏。
 */
public final class WinToast {

    private static final Logger LOGGER = LoggerFactory.getLogger("BabyzombieAddons/WinToast");

    private static final boolean IS_WINDOWS =
        System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    /** 通知归属标识:须与开始菜单快捷方式写入的 AppUserModelID 一致,且长期不变(改动会断旧通知归属) */
    private static final String AUMID = "BabyzombieAddons";

    private static final String POWERSHELL = resolvePowershellPath();

    /** schtasks.exe 路径(提权时经计划任务降权执行 PowerShell 用;runas /trustlevel 实测不会降权) */
    private static final String SCHTASKS = resolveSchtasksPath();

    /** 计划任务名(降权执行) */
    private static final String TASK_NAME = "BabyzombieAddonsToast";

    /** 开始菜单快捷方式路径(会话级注册:游戏退出时删除,下次启动自动重建) */
    private static final String LNK_PATH = resolveLnkPath();

    /** 降权执行的命令载体:schtasks /tr 有 261 字符上限,长命令写入此 .cmd 文件再让任务执行 */
    private static final String CMD_FILE = System.getProperty("java.io.tmpdir") + "\\babyzombieaddons-toast.cmd";

    /** 注册/修复完成的标记文件(任务异步执行,用于轮询等待) */
    private static final String DONE_FILE = System.getProperty("java.io.tmpdir") + "\\babyzombieaddons-done";

    private static final int TOKEN_QUERY = 0x0008;
    private static final int TOKEN_ELEVATION_CLASS = 20;

    /** 注册是否尝试过(每 JVM 会话至多一次) */
    private static final AtomicBoolean REGISTER_ATTEMPTED = new AtomicBoolean();

    private static final AtomicBoolean NOT_SUPPORTED_LOGGED = new AtomicBoolean();

    /** 图标文件 URI;null=未计算,""=提取失败(均只在执行器线程读写) */
    private static String iconUri;

    /** 单线程串行发送,避免并发拉起多个 PowerShell 进程;注册与发送同队列,天然有序 */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "BZA-Toast");
        thread.setDaemon(true);
        return thread;
    });

    /** 注册脚本内嵌的 C# 帮助类:经 IPropertyStore 手工布局 PROPVARIANT 写入 AppUserModelID(绕开 COM 结构体编组) */
    private static final String AUMID_CSHARP = """
        using System;
        using System.Runtime.InteropServices;

        public static class ShortcutAumid
        {
            [ComImport, Guid("886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
            interface IPropertyStore
            {
                int GetCount(out uint cProps);
                int GetAt(uint iProp, out PROPERTYKEY pkey);
                int GetValue(ref PROPERTYKEY key, IntPtr pv);
                int SetValue(ref PROPERTYKEY key, IntPtr pv);
                int Commit();
            }

            [StructLayout(LayoutKind.Sequential)]
            struct PROPERTYKEY
            {
                public Guid fmtid;
                public uint pid;
            }

            [DllImport("shell32.dll", CharSet = CharSet.Unicode)]
            static extern int SHGetPropertyStoreFromParsingName(string pszPath, IntPtr pbc, int flags, ref Guid riid, out IPropertyStore ppv);

            [DllImport("ole32.dll")]
            static extern int PropVariantClear(IntPtr pvar);

            static int SetProp(IPropertyStore store, Guid fmtid, uint pid, string value)
            {
                PROPERTYKEY key = new PROPERTYKEY();
                key.fmtid = fmtid;
                key.pid = pid;
                IntPtr pv = Marshal.AllocCoTaskMem(24);
                try
                {
                    Marshal.WriteInt16(pv, 0, 31);
                    Marshal.WriteInt16(pv, 2, 0);
                    Marshal.WriteInt16(pv, 4, 0);
                    Marshal.WriteInt16(pv, 6, 0);
                    IntPtr str = Marshal.StringToCoTaskMemUni(value);
                    Marshal.WriteIntPtr(pv, 8, str);
                    int hr = store.SetValue(ref key, pv);
                    if (hr == 0) hr = store.Commit();
                    PropVariantClear(pv);
                    return hr;
                }
                finally
                {
                    Marshal.FreeCoTaskMem(pv);
                }
            }

            public static int SetAppUserModelID(string lnkPath, string appId)
            {
                Guid iid = new Guid("886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99");
                IPropertyStore store = null;
                int hr = SHGetPropertyStoreFromParsingName(lnkPath, IntPtr.Zero, 2, ref iid, out store);
                if (hr != 0 || store == null) return hr;
                try
                {
                    return SetProp(store, new Guid("9F4C2855-9F79-4B39-A8D0-E1D42DE1D5F3"), 5, appId);
                }
                finally
                {
                    Marshal.ReleaseComObject(store);
                }
            }

            // System.AppUserModel.Hidden (pid 9, VT_BOOL): 从开始菜单"所有应用"隐藏该快捷方式,
            // 但 AUMID 注册仍然有效 —— 与文件 Hidden 属性不同(隐藏文件会被 Win10 枚举跳过导致 toast 静默丢弃)。
            public static int SetAppUserModelHidden(string lnkPath, bool hidden)
            {
                Guid iid = new Guid("886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99");
                IPropertyStore store = null;
                int hr = SHGetPropertyStoreFromParsingName(lnkPath, IntPtr.Zero, 2, ref iid, out store);
                if (hr != 0 || store == null) return hr;
                try
                {
                    PROPERTYKEY key = new PROPERTYKEY();
                    key.fmtid = new Guid("9F4C2855-9F79-4B39-A8D0-E1D42DE1D5F3");
                    key.pid = 9;
                    IntPtr pv = Marshal.AllocCoTaskMem(24);
                    try
                    {
                        Marshal.WriteInt16(pv, 0, 11); // VT_BOOL
                        Marshal.WriteInt16(pv, 2, 0);
                        Marshal.WriteInt16(pv, 4, 0);
                        Marshal.WriteInt16(pv, 6, 0);
                        Marshal.WriteInt16(pv, 8, hidden ? (short)-1 : (short)0);
                        hr = store.SetValue(ref key, pv);
                        if (hr == 0) hr = store.Commit();
                        PropVariantClear(pv);
                        return hr;
                    }
                    finally { Marshal.FreeCoTaskMem(pv); }
                }
                finally { Marshal.ReleaseComObject(store); }
            }
        }
        """;

    private WinToast() {}

    /** 当前系统是否支持(仅 Windows)。 */
    public static boolean isSupported() {
        return IS_WINDOWS;
    }

    /** 游戏启动时调用:后台异步完成 AUMID 注册,避免首次发送时注册未完成导致弹窗被丢;并启动通知点击信号监听。非 Windows 直接跳过。 */
    public static void init() {
        if (!IS_WINDOWS) {
            return;
        }
        ToastActionListener.start();
        EXECUTOR.execute(WinToast::ensureRegistered);
    }

    /**
     * 游戏退出时调用:删除开始菜单快捷方式(会话级注册,下次启动自动重建)。
     * 游戏运行期间快捷方式已通过 System.AppUserModel.Hidden 从开始菜单隐藏(注册不受影响),退出即删除,不留痕迹。
     */
    public static void shutdown() {
        if (!IS_WINDOWS) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(LNK_PATH));
        } catch (Exception e) {
            LOGGER.debug("Failed to remove toast shortcut", e);
        }
    }

    /**
     * 发送一条系统通知(异步,不阻塞调用线程)。
     * 标题与正文任意文本均可(自动做 XML / PowerShell 转义),失败仅记日志;相同内容 5 秒内重复发送会被合并。
     */
    public static void send(String title, String message) {
        if (!IS_WINDOWS) {
            if (NOT_SUPPORTED_LOGGED.compareAndSet(false, true)) {
                LOGGER.warn("System notification is only supported on Windows, ignored");
            }
            return;
        }
        EXECUTOR.execute(() -> {
            try {
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
                ensureRegistered();
                String script = buildScript(title, message);
                if (!executePowerShell(script, false, 0)) {
                    LOGGER.warn("Failed to start powershell for system notification, lastError={}",
                        Kernel32.INSTANCE.GetLastError());
                } else {
                    LOGGER.debug("System notification sent: {}", title);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to send system notification", e);
            }
        });
    }

    /** 合并节流锁与状态(仅执行器线程访问) */
    private static final Object SEND_LOCK = new Object();
    private static final long COALESCE_MS = 5000;
    private static String lastToastKey;
    private static long lastToastTime;

    /**
     * 修复系统通知(异步):通知服务状态损坏时(弹窗只记录不显示),重置 wpndatabase 通知数据库并重启服务,
     * 完成后自动发一条测试通知。会清空通知中心历史。修复期间服务不可用约 10 秒。
     */
    public static void repair() {
        if (!IS_WINDOWS) {
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                String script = buildRepairScript();
                if (!executePowerShell(script, true, 60000)) {
                    LOGGER.warn("Failed to start powershell for notification repair, lastError={}",
                        Kernel32.INSTANCE.GetLastError());
                }
                send("BabyzombieAddons", "System notification repair complete");
            } catch (Exception e) {
                LOGGER.warn("Failed to repair system notification", e);
            }
        });
    }

    /** 生成修复脚本:停 WpnUserService(轮询到完全停止,避免半停止状态移库) → 备份并移除 wpndatabase* → 启动服务并检查结果 → 写完成标记。 */
    private static String buildRepairScript() {
        return """
            $ErrorActionPreference = 'Continue'
            try {
                $svc = Get-Service -Name 'WpnUserService*' | Select-Object -First 1
                if ($svc) {
                    Stop-Service -Name $svc.Name -Force -ErrorAction SilentlyContinue
                    $deadline = (Get-Date).AddSeconds(15)
                    do { Start-Sleep -Milliseconds 500; $svc.Refresh() } while ($svc.Status -ne 'Stopped' -and (Get-Date) -lt $deadline)
                    Start-Sleep -Seconds 2
                    $dbDir = Join-Path $env:LOCALAPPDATA 'Microsoft\\Windows\\Notifications'
                    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
                    Get-ChildItem $dbDir -Filter 'wpndatabase*' -File -ErrorAction SilentlyContinue | Where-Object {
                        $_.Name -notlike '*.bak-*'
                    } | ForEach-Object {
                        Move-Item $_.FullName ($_.FullName + '.bak-' + $stamp) -Force -ErrorAction SilentlyContinue
                    }
                    Start-Service -Name $svc.Name -ErrorAction SilentlyContinue
                    $deadline = (Get-Date).AddSeconds(20)
                    do { Start-Sleep -Milliseconds 500; $svc.Refresh() } while ($svc.Status -ne 'Running' -and (Get-Date) -lt $deadline)
                    if ($svc.Status -ne 'Running') {
                        ('Repair: WpnUserService failed to start, status=' + $svc.Status) | Out-File -Append -FilePath (Join-Path $env:TEMP 'babyzombieaddons-toast.log') -Encoding utf8
                    }
                    Start-Sleep -Seconds 5
                }
            } catch { }
            [System.IO.File]::WriteAllText((Join-Path $env:TEMP 'babyzombieaddons-done'), 'done')
            """;
    }

    /** AUMID 注册:建开始菜单快捷方式并写入 AppUserModelID;等待注册完成后再发通知。失败不阻断发送(Win11 无需注册)。 */
    private static void ensureRegistered() {
        if (!REGISTER_ATTEMPTED.compareAndSet(false, true)) {
            return;
        }
        try {
            String script = buildRegisterScript();
            if (!executePowerShell(script, true, 30000)) {
                LOGGER.warn("Failed to start powershell for AUMID registration, lastError={}",
                    Kernel32.INSTANCE.GetLastError());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to register AUMID for system notification", e);
        }
    }

    /**
     * 执行 PowerShell 脚本(含 -EncodedCommand)。
     * 本进程提权时经计划任务(RL LIMITED)降权执行:Win10 会把提权进程的通知「只记录不显示」,
     * 且实测 runas /trustlevel:0x20000 不会真正降权(子进程仍是 High,还会损坏通知服务);
     * 计划任务以受限令牌运行 = 普通用户完整性。非提权环境直接执行。
     * waitDone=true 时轮询脚本写入的完成标记(任务为异步执行)。
     */
    private static boolean executePowerShell(String script, boolean waitDone, long timeoutMs) {
        String encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
        String psCmd = "\"" + POWERSHELL + "\" -NoProfile -NonInteractive -WindowStyle Hidden"
            + " -ExecutionPolicy Bypass -EncodedCommand " + encoded;
        boolean spawned;
        if (!isProcessElevated()) {
            spawned = Win32Api.createProcessHidden(psCmd);
            LOGGER.debug("WinToast: not elevated, direct path spawned={}", spawned);
        } else {
            try {
                Files.writeString(Path.of(CMD_FILE), psCmd, StandardCharsets.UTF_8);
            } catch (Exception e) {
                LOGGER.warn("Failed to write toast cmd file", e);
                return false;
            }
            boolean createOk = Win32Api.createProcessHidden(
                "\"" + SCHTASKS + "\" /create /tn " + TASK_NAME + " /tr \"" + CMD_FILE
                    + "\" /sc once /st 00:00 /rl LIMITED /it /f");
            boolean runOk = Win32Api.createProcessHidden("\"" + SCHTASKS + "\" /run /tn " + TASK_NAME);
            Win32Api.createProcessHidden("\"" + SCHTASKS + "\" /delete /tn " + TASK_NAME + " /f");
            spawned = createOk && runOk;
            LOGGER.debug("WinToast: elevated, task path createOk={} runOk={}", createOk, runOk);
        }
        if (!spawned || !waitDone) {
            return spawned;
        }
        // 轮询完成标记(异步执行场景无法直接等进程)
        try {
            Path done = Path.of(DONE_FILE);
            Files.deleteIfExists(done);
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                if (Files.exists(done)) {
                    Files.deleteIfExists(done);
                    return true;
                }
                Thread.sleep(300);
            }
            LOGGER.warn("Timed out waiting for powershell completion");
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            LOGGER.warn("Failed to check powershell completion", e);
            return false;
        }
    }

    /** 当前进程是否提权(结果缓存;仅执行器线程访问)。 */
    private static boolean isProcessElevated() {
        if (elevated == null) {
            elevated = detectElevated();
        }
        return elevated;
    }

    private static boolean detectElevated() {
        IntByReference token = new IntByReference();
        if (!Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(), TOKEN_QUERY, token)) {
            return false;
        }
        try {
            TOKEN_ELEVATION info = new TOKEN_ELEVATION();
            IntByReference length = new IntByReference();
            boolean ok = Advapi32.INSTANCE.GetTokenInformation(token, TOKEN_ELEVATION_CLASS, info, info.size(), length);
            return ok && info.TokenIsElevated != 0;
        } finally {
            Kernel32.INSTANCE.CloseHandle(new Pointer(token.getValue()));
        }
    }

    private static volatile Boolean elevated;

    /**
     * 生成 AUMID 注册脚本:快捷方式缺失则创建,存在但 target/参数与当前不一致则刷新;
     *  再经 C# 帮助类写入 AppUserModelID。用 File.Exists 判断(Test-Path 看不见隐藏文件会重复重建)。
     *  快捷方式 target 指向 PowerShell + 内联 UDP 命令:点击系统通知时,系统启动该快捷方式,
     *  即执行「恢复游戏窗口」信号(回环 UDP,端口 48236,由 ToastActionListener 接收)。
     *  快捷方式文件必须保持可见:Win10 枚举开始菜单时会跳过隐藏文件,导致 AUMID 注册失效、通知被静默丢弃
     *  (实测:隐藏 lnk → Get-StartApps 无此项 → toast 只记录不显示)。
     *  从开始菜单"隐藏"改用 System.AppUserModel.Hidden 属性(pid 9):开始菜单不显示,但注册仍然有效
     *  (19045 实测该属性不生效,保留以兼容其他版本)。
     *  注册完成后重启 WpnUserService:系统对开始菜单快捷方式的枚举有缓存,新建/变更 lnk 后需重启服务才会重新枚举投递。
     */
    private static String buildRegisterScript() {
        String script = """
            $ErrorActionPreference = 'Stop'
            $lnkDir = [Environment]::GetFolderPath('Programs')
            $lnkPath = Join-Path $lnkDir '<AUMID>.lnk'
            try {
                $WshShell = New-Object -ComObject WScript.Shell
                $psExe = Join-Path $env:SystemRoot 'System32\\WindowsPowerShell\\v1.0\\powershell.exe'
                $udpArgs = '-NoProfile -NonInteractive -WindowStyle Hidden -Command $u=New-Object Net.Sockets.UdpClient;$u.Connect(''127.0.0.1'',48236);$u.Send([byte[]](66,90,65,1,1),5)'
                $sc = $WshShell.CreateShortcut($lnkPath)
                if ($sc.TargetPath -ne $psExe -or $sc.Arguments -ne $udpArgs) {
                    $sc.TargetPath = $psExe
                    $sc.Arguments = $udpArgs
                    $sc.Save()
                }
                Add-Type -TypeDefinition @'
            <AUMID_CSHARP>
            '@
                $hr = [ShortcutAumid]::SetAppUserModelID($lnkPath, '<AUMID>')
                if ($hr -ne 0) { throw ('SetAppUserModelID hr=0x' + $hr.ToString('X8')) }
                $hr2 = [ShortcutAumid]::SetAppUserModelHidden($lnkPath, $true)
                if ($hr2 -ne 0) { throw ('SetAppUserModelHidden hr=0x' + $hr2.ToString('X8')) }
                $attrs = [System.IO.File]::GetAttributes($lnkPath)
                [System.IO.File]::SetAttributes($lnkPath, $attrs -band (-bnot [System.IO.FileAttributes]::Hidden))
                $svc = Get-Service -Name 'WpnUserService*' | Select-Object -First 1
                if ($svc) {
                    Stop-Service -Name $svc.Name -Force -ErrorAction SilentlyContinue
                    $deadline = (Get-Date).AddSeconds(15)
                    do { Start-Sleep -Milliseconds 500; $svc.Refresh() } while ($svc.Status -ne 'Stopped' -and (Get-Date) -lt $deadline)
                    Start-Service -Name $svc.Name -ErrorAction SilentlyContinue
                    $deadline = (Get-Date).AddSeconds(20)
                    do { Start-Sleep -Milliseconds 500; $svc.Refresh() } while ($svc.Status -ne 'Running' -and (Get-Date) -lt $deadline)
                }
            } catch {
                $_ | Out-File -Append -FilePath (Join-Path $env:TEMP 'babyzombieaddons-toast.log') -Encoding utf8
            }
            [System.IO.File]::WriteAllText((Join-Path $env:TEMP 'babyzombieaddons-done'), 'done')
            """;
        return script
            .replace("<AUMID>", AUMID)
            .replace("<AUMID_CSHARP>", AUMID_CSHARP);
    }

    /** 生成 PowerShell 脚本:加载 WinRT 类型 → 构造 Toast XML(含 appLogoOverride 图标)→ 弹出。失败原因落盘到 %TEMP%。 */
    private static String buildScript(String title, String message) {
        String image = "";
        String uri = toastIconUri();
        if (uri != null) {
            image = "<image placement=\"appLogoOverride\" src=\"" + escapeXml(uri) + "\"/>";
        }
        String xml = "<toast><visual><binding template=\"ToastGeneric\">"
            + image
            + "<text>" + escapeXml(title) + "</text>"
            + "<text>" + escapeXml(message) + "</text>"
            + "</binding></visual></toast>";
        // XML 已无单引号残留,再转义一次 PS 单引号字符串即可安全内嵌
        String quotedXml = xml.replace("'", "''");
        return "try { "
            + "[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null; "
            + "[Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType = WindowsRuntime] | Out-Null; "
            + "$xml = New-Object Windows.Data.Xml.Dom.XmlDocument; "
            + "$xml.LoadXml('" + quotedXml + "'); "
            + "$toast = New-Object Windows.UI.Notifications.ToastNotification $xml; "
            + "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('" + AUMID + "').Show($toast); "
            + "} catch { $_ | Out-File -Append -FilePath (Join-Path $env:TEMP 'babyzombieaddons-toast.log') -Encoding utf8 }";
    }

    /** 备用图标提供者(由游戏层注入:从 MC 资源管理器读取 mod 图标,兼容 dev 环境 classpath 资源缺失) */
    private static volatile java.util.function.Supplier<java.io.InputStream> modIconProvider;

    /** 注入备用图标提供者(游戏层调用;取不到返回 null)。 */
    public static void setModIconProvider(java.util.function.Supplier<java.io.InputStream> provider) {
        modIconProvider = provider;
    }

    /** 提取通知左上角图标:优先抓取游戏窗口图标(任务栏同款),失败回退 mod 自带 icon.png;再失败返回 null(系统用快捷方式图标兜底)。 */
    private static String toastIconUri() {
        if (iconUri == null) {
            Path out = Path.of(System.getProperty("java.io.tmpdir"), "babyzombieaddons-toast-icon.png");
            iconUri = WindowIcon.extractIconToPng(out);
            if (iconUri == null) {
                iconUri = extractModIcon(out);
            }
        }
        return iconUri.isEmpty() ? null : iconUri;
    }

    /** 从 mod 资源提取 icon.png 到 out 并返回 file:// URI;失败返回 ""。 */
    private static String extractModIcon(Path out) {
        try (InputStream in = extractModIconStream()) {
            if (in == null) {
                LOGGER.warn("Mod icon extraction failed: not found on classpath and no provider");
                return "";
            }
            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
            return out.toUri().toString();
        } catch (Exception e) {
            LOGGER.warn("Failed to extract mod icon", e);
            return "";
        }
    }

    private static InputStream extractModIconStream() {
        InputStream in = WinToast.class.getResourceAsStream("/assets/babyzombieaddons/icon.png");
        if (in == null) {
            java.util.function.Supplier<InputStream> provider = modIconProvider;
            if (provider != null) {
                in = provider.get();
            }
        }
        return in;
    }

    /** XML 文本/属性转义(& 优先),并把换行压成空格(保持 PS 命令行为单行)。 */
    private static String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace("\r", " ")
            .replace("\n", " ");
    }

    private static String resolvePowershellPath() {
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot == null || systemRoot.isBlank()) {
            systemRoot = "C:\\Windows";
        }
        return systemRoot + "\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
    }

    private static String resolveSchtasksPath() {
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot == null || systemRoot.isBlank()) {
            systemRoot = "C:\\Windows";
        }
        return systemRoot + "\\System32\\schtasks.exe";
    }

    private static String resolveLnkPath() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            appData = System.getProperty("user.home") + "\\AppData\\Roaming";
        }
        return appData + "\\Microsoft\\Windows\\Start Menu\\Programs\\" + AUMID + ".lnk";
    }
}
