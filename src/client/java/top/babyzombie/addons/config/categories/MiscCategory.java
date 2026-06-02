package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfig.ConfigBackend;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class MiscCategory {
    private MiscCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(net.minecraft.resources.Identifier.fromNamespaceAndPath("babyzombieaddons", "config/misc"))
                .name(Component.translatable("config.babyzombieaddons.category.misc"))
                .option(Option.<ConfigBackend>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.configBackend"))
                        .description(Component.translatable("config.babyzombieaddons.option.configBackend.desc"))
                        .binding(defaults.debug.configBackend,
                                () -> config.debug.configBackend,
                                v -> config.debug.configBackend = v)
                        .controller(ConfigUtils.createEnumController(b -> switch (b) {
                            case YACL -> Component.literal("YACL");
                            case MOUL_CONFIG -> Component.literal("MoulConfig");
                        }))
                        .build())
                .option(bool("debugMode", defaults.debug.debugMode,
                        () -> config.debug.debugMode, v -> config.debug.debugMode = v))
                .build();
    }

    private static Option<Boolean> bool(String key, boolean def, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("config.babyzombieaddons.option." + key))
                .description(Component.translatable("config.babyzombieaddons.option." + key + ".desc"))
                .binding(def, getter, setter)
                .controller(ConfigUtils.createBooleanController())
                .build();
    }
}
