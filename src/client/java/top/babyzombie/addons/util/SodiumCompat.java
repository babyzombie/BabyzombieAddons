package top.babyzombie.addons.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;

/**
 * Sodium 视频设置入口兼容辅助类。
 * <p>
 * 装了 Sodium 时视频设置按钮应打开 Sodium 的 VideoSettingsScreen（装 Reese's
 * Sodium Options 时由 RSO 的 mixin 自动接管替换成 RSO 界面，无需额外处理）；
 * 未装 Sodium 时退回原版界面。所有 Sodium 类型引用隔离在 {@link SodiumBridge}，
 * 确保 Sodium 未安装时不会触发 {@link NoClassDefFoundError}。
 */
public final class SodiumCompat {

    private SodiumCompat() {}

    /**
     * 创建视频设置屏幕：Sodium 已装走 Sodium 入口，否则原版入口。
     *
     * @param parent 打开前的当前屏幕（用于返回）
     */
    public static Screen createVideoSettingsScreen(Screen parent) {
        if (SodiumBridge.LOADED) return SodiumBridge.createScreen(parent);
        var mc = Minecraft.getInstance();
        return new VideoSettingsScreen(parent, mc, mc.options);
    }

    /**
     * 内部桥接类 —— 仅当 Sodium 已加载时才会被 JVM 加载，
     * 避免在 Sodium 未安装时因类型解析失败而崩溃。
     */
    private static final class SodiumBridge {

        static final boolean LOADED = FabricLoader.getInstance().isModLoaded("sodium");

        static Screen createScreen(Screen parent) {
            return net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen.createScreen(parent);
        }
    }
}
