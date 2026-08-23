package top.babyzombie.addons.util.win32;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.babyzombie.addons.util.win32.Win32Api.BITMAP;
import top.babyzombie.addons.util.win32.Win32Api.BITMAPINFO;
import top.babyzombie.addons.util.win32.Win32Api.Gdi32;
import top.babyzombie.addons.util.win32.Win32Api.ICONINFO;
import top.babyzombie.addons.util.win32.Win32Api.User32;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * 抓取游戏窗口的图标(即任务栏显示的图标)并保存为 PNG(仅 Windows)。
 * <p>
 * 流程:WM_GETICON(ICON_BIG)取窗口 HICON → GetIconInfo 取颜色位图 → GetDIBits 拷贝 BGRA 像素 →
 * Java 侧转 ARGB 后用 ImageIO 写 PNG。窗口图标归窗口所有,绝不 DestroyIcon;GetIconInfo 返回的位图由本类 DeleteObject。
 * 任何一步失败都返回 null,由调用方降级(如回退 mod 自带图标)。
 */
public final class WindowIcon {

    private static final Logger LOGGER = LoggerFactory.getLogger("BabyzombieAddons/WindowIcon");

    private WindowIcon() {}

    /** 抓取本进程主窗口图标并写入 out,返回 file:// URI;失败返回 null 并记 WARN(定位游戏内提取失败原因)。 */
    public static String extractIconToPng(Path out) {
        Pointer hwnd = Win32Api.findMainWindow();
        if (hwnd == null || Pointer.nativeValue(hwnd) == 0) {
            LOGGER.warn("Window icon extraction failed: no main window found");
            return null;
        }
        return extractIconToPng(hwnd, out);
    }

    /** 抓取指定窗口的图标并写入 out,返回 file:// URI;失败返回 null。 */
    public static String extractIconToPng(Pointer hwnd, Path out) {
        try {
            Pointer hIcon = iconOf(hwnd);
            if (hIcon == null) {
                return null;
            }
            ICONINFO info = new ICONINFO();
            if (!User32.INSTANCE.GetIconInfo(hIcon, info)) {
                LOGGER.debug("GetIconInfo failed");
                return null;
            }
            try {
                if (info.hbmColor == null || Pointer.nativeValue(info.hbmColor) == 0) {
                    return null;
                }
                BITMAP bmp = new BITMAP();
                if (Gdi32.INSTANCE.GetObjectW(info.hbmColor, bmp.size(), bmp.getPointer()) == 0) {
                    return null;
                }
                bmp.read(); // 经 getPointer() 传入绕过了 JNA 自动回读,须手动同步
                int width = bmp.bmWidth;
                int height = bmp.bmHeight;
                if (width <= 0 || height <= 0 || width > 512 || height > 512) {
                    return null;
                }
                Pointer hdc = Gdi32.INSTANCE.CreateCompatibleDC(null);
                if (hdc == null) {
                    return null;
                }
                try {
                    BITMAPINFO bmi = new BITMAPINFO();
                    bmi.bmiHeader.biSize = bmi.bmiHeader.size();
                    bmi.bmiHeader.biWidth = width;
                    bmi.bmiHeader.biHeight = -height; // 负数 = 自顶向下
                    bmi.bmiHeader.biPlanes = 1;
                    bmi.bmiHeader.biBitCount = 32;
                    long pixelBytes = (long) width * height * 4;
                    Memory bits = new Memory(pixelBytes);
                    int lines = Gdi32.INSTANCE.GetDIBits(hdc, info.hbmColor, 0, height, bits, bmi, 0);
                    if (lines != height) {
                        return null;
                    }
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    byte[] raw = bits.getByteArray(0, (int) pixelBytes);
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int i = (y * width + x) * 4;
                            int b = raw[i] & 0xFF;
                            int g = raw[i + 1] & 0xFF;
                            int r = raw[i + 2] & 0xFF;
                            int a = raw[i + 3] & 0xFF;
                            image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                        }
                    }
                    ImageIO.write(image, "png", out.toFile());
                    return out.toUri().toString();
                } finally {
                    Gdi32.INSTANCE.DeleteDC(hdc);
                }
            } finally {
                if (info.hbmColor != null) {
                    Gdi32.INSTANCE.DeleteObject(info.hbmColor);
                }
                if (info.hbmMask != null) {
                    Gdi32.INSTANCE.DeleteObject(info.hbmMask);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to extract window icon", e);
            return null;
        }
    }

    /** 取窗口大图标:WM_GETICON(ICON_BIG),失败回退类图标 GCLP_HICON。 */
    private static Pointer iconOf(Pointer hwnd) {
        long result = User32.INSTANCE.SendMessageW(hwnd, Win32Api.WM_GETICON, Win32Api.ICON_BIG, 0);
        if (result == 0) {
            result = User32.INSTANCE.GetClassLongPtrW(hwnd, Win32Api.GCLP_HICON);
        }
        return result == 0 ? null : new Pointer(result);
    }
}
