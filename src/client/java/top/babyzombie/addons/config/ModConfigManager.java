package top.babyzombie.addons.config;

import net.azureaaron.dandelion.api.ConfigManager;
import net.azureaaron.dandelion.api.ConfigType;
import net.azureaaron.dandelion.api.DandelionConfigScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.config.categories.*;

import java.nio.file.Path;
import java.util.function.UnaryOperator;

public final class ModConfigManager {

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("babyzombieaddons").resolve("settings.json");

    private static final ConfigManager<ModConfig> CONFIG_MANAGER = ConfigManager.create(
            ModConfig.class, CONFIG_FILE, UnaryOperator.identity()
    );

    private ModConfigManager() {}

    public static void init() {
        CONFIG_MANAGER.load();
    }

    public static ModConfig get() {
        return CONFIG_MANAGER.instance();
    }

    public static ModConfig getUnpatched() {
        return CONFIG_MANAGER.unpatchedInstance();
    }

    public static void save() {
        CONFIG_MANAGER.save();
    }

    public static Screen createGUI(@Nullable Screen parent) {
        return createGUI(parent, "");
    }

    public static Screen createGUI(@Nullable Screen parent, String search) {
        return DandelionConfigScreen.create(CONFIG_MANAGER, (defaults, config, builder) -> builder
                .title(Component.translatable("config.babyzombieaddons.title"))
                .category(GeneralCategory.create(defaults, config))
                .category(DungeonCategory.create(defaults, config))
                .category(KuudraCategory.create(defaults, config))
                .category(SlayerCategory.create(defaults, config))
                .category(MiningCategory.create(defaults, config))
                .category(GardenCategory.create(defaults, config))
                .category(PartyCategory.create(defaults, config))
                .category(PopupCategory.create(defaults, config))
                .category(EventsCategory.create(defaults, config))
                .category(MiscCategory.create(defaults, config))
                .search(search)
        ).generateScreen(parent, get().debug.configBackend == ModConfig.ConfigBackend.YACL ? ConfigType.YACL : ConfigType.MOUL_CONFIG);
    }
}
