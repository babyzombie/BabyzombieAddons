package top.babyzombie.addons.config;

import net.azureaaron.dandelion.api.ConfigManager;
import net.azureaaron.dandelion.api.ConfigType;
import net.azureaaron.dandelion.api.DandelionConfigScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.config.ModConfig.ConfigBackend;
import top.babyzombie.addons.config.categories.*;
import top.babyzombie.addons.util.ChatUtils;

import java.nio.file.Path;
import java.util.function.UnaryOperator;

public final class ModConfigManager {

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("babyzombieaddons").resolve("settings.json");

    private static final ConfigManager<ModConfig> CONFIG_MANAGER = ConfigManager.create(
            ModConfig.class, CONFIG_FILE, UnaryOperator.identity()
    );

    private static final boolean YACL_LOADED = FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3");

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
        ConfigType configType;
        if (get().debug.configBackend == ConfigBackend.YACL) {
            if (YACL_LOADED) {
                configType = ConfigType.YACL;
            } else {
                configType = ConfigType.MOUL_CONFIG;
                ChatUtils.showMessage(Component.translatable("config.babyzombieaddons.yacl_missing").getString());
            }
        } else {
            configType = ConfigType.MOUL_CONFIG;
        }

        return DandelionConfigScreen.create(CONFIG_MANAGER, (defaults, config, builder) -> builder
                .title(Component.translatable("config.babyzombieaddons.title"))
                .category(GeneralCategory.create(defaults, config))
                .category(SkyblockCategory.create(defaults, config))
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
        ).generateScreen(parent, configType);
    }
}
