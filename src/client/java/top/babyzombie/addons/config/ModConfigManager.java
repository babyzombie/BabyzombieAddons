package top.babyzombie.addons.config;

import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.managed.ManagedConfig;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public final class ModConfigManager {

    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("babyzombieaddons").resolve("settings.json").toFile();

    private static final ManagedConfig<ModConfig> MANAGED_CONFIG = ManagedConfig.create(
            CONFIG_FILE, ModConfig.class
    );

    private ModConfigManager() {}

    public static void init() {
        MANAGED_CONFIG.reloadFromFile();
    }

    public static ModConfig get() {
        return MANAGED_CONFIG.getInstance();
    }

    public static void save() {
        MANAGED_CONFIG.saveToFile();
    }

    public static Screen createGUI(@Nullable Screen parent) {
        return createGUI(parent, "");
    }

    public static Screen createGUI(@Nullable Screen parent, String search) {
        MANAGED_CONFIG.rebuildConfigProcessor();
        MANAGED_CONFIG.openConfigGui();
        // Set wide mode after screen creation (editor layout is built during render init)
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof MoulConfigScreenComponent msc) {
            if (msc.getGuiContext().getRoot() instanceof GuiElementComponent gec
                    && gec.getElement() instanceof MoulConfigEditor<?> editor) {
                editor.wide = get().misc.wideMoulConfig;
            }
        }
        return screen;
    }
}
