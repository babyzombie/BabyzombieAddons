package top.babyzombie.addons.util;

import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

/**
 * 打开其他模组的配置屏幕（经 ModMenu 的 "modmenu" entrypoint 取配置工厂）。
 * ModMenu 类型引用隔离在 {@link ModMenuBridge}，ModMenu 未安装时不会触发
 * {@link NoClassDefFoundError}。
 */
public final class ModConfigOpener {

    private ModConfigOpener() {}

    /**
     * 打开指定 mod 的配置屏幕；ModMenu 未装或该 mod 无配置页面时原样返回 parent
     * （保持当前屏幕，避免 setScreen(null) 关闭界面）。
     */
    public static Screen createScreen(String modId, Screen parent) {
        if (!FabricLoader.getInstance().isModLoaded("modmenu")) return parent;
        var screen = ModMenuBridge.createScreen(modId, parent);
        return screen != null ? screen : parent;
    }

    private static final class ModMenuBridge {

        static Screen createScreen(String modId, Screen parent) {
            for (var container : FabricLoader.getInstance()
                    .getEntrypointContainers("modmenu", ModMenuApi.class)) {
                if (container.getProvider().getMetadata().getId().equals(modId)) {
                    var factory = container.getEntrypoint().getModConfigScreenFactory();
                    return factory == null ? null : factory.create(parent);
                }
            }
            return null;
        }
    }
}
