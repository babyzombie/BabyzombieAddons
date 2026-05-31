package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ButtonOption;
import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.HudManager;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfig.ConfigBackend;

public final class DebugCategory {
    private DebugCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.babyzombieaddons.category.debug"))
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.hudEdit"))
                        .action(screen -> HudManager.toggleEditMode())
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.configBackend"))
                        .collapsed(true)
                        .option(Option.<ConfigBackend>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.configBackend"))
                                .binding(defaults.debug.configBackend,
                                        () -> config.debug.configBackend,
                                        v -> config.debug.configBackend = v)
                                .controller(ConfigUtils.createEnumController(b -> switch (b) {
                                    case YACL -> Component.literal("YACL");
                                    case MOUL_CONFIG -> Component.literal("MoulConfig");
                                }))
                                .build())
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.debugMode"))
                        .binding(defaults.debug.debugMode,
                                () -> config.debug.debugMode,
                                v -> config.debug.debugMode = v)
                        .controller(ConfigUtils.createBooleanController())
                        .build())
                .build();
    }
}
