package top.babyzombie.addons.config;

import net.azureaaron.dandelion.api.ConfigManager;
import net.azureaaron.dandelion.api.ConfigType;
import net.azureaaron.dandelion.api.DandelionConfigScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.config.ModConfig.ConfigBackend;
import top.babyzombie.addons.config.categories.*;

import java.nio.file.Path;
import java.util.function.UnaryOperator;

public final class ModConfigManager {

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("babyzombieaddons").resolve("settings.json");

    private static final ConfigManager<ModConfig> CONFIG_MANAGER = ConfigManager.create(
            ModConfig.class, CONFIG_FILE, UnaryOperator.identity()
    );

    private static final boolean YACL_LOADED = FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3");
    private static final boolean FLK_LOADED = FabricLoader.getInstance().isModLoaded("fabric-language-kotlin");

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
        try {
            ConfigType configType = get().debug.configBackend == ConfigBackend.YACL ? ConfigType.YACL : ConfigType.MOUL_CONFIG;

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
        } catch (Exception e) {
            return new MissingDepScreen(parent, e);
        }
    }

    private static final class MissingDepScreen extends Screen {
        private final Screen parent;
        private final Exception error;

        MissingDepScreen(Screen parent, Exception error) {
            super(Component.translatable("babyzombieaddons.missing_deps.title"));
            this.parent = parent;
            this.error = error;
        }

        @Override
        protected void init() {
            addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, btn ->
                    minecraft.setScreen(parent)
            ).bounds(width / 2 - 100, height / 4 + 80, 200, 20).build());
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            super.extractRenderState(graphics, mouseX, mouseY, delta);
            int y = height / 4 - 20;
            graphics.centeredText(font, title, width / 2, y, 0xFF5555);
            y += 24;
            if (error != null) {
                String errorMsg = error.toString();
                if (errorMsg.length() > 80) {
                    errorMsg = errorMsg.substring(0, 77) + "...";
                }
                graphics.centeredText(font,
                        Component.translatable("babyzombieaddons.missing_deps.error", errorMsg),
                        width / 2, y, 0xFFAAAAAA);
                y += 18;
            }
            graphics.centeredText(font,
                    Component.translatable("babyzombieaddons.missing_deps.hint"),
                    width / 2, y, 0xCCCCCC);
            y += 16;
            if (!YACL_LOADED) {
                graphics.centeredText(font,
                        Component.translatable("babyzombieaddons.missing_deps.yacl"),
                        width / 2, y, 0xFFAAAAAA);
                y += 14;
            }
            if (!FLK_LOADED) {
                graphics.centeredText(font,
                        Component.translatable("babyzombieaddons.missing_deps.flk"),
                        width / 2, y, 0xFFAAAAAA);
            }
        }
    }
}
