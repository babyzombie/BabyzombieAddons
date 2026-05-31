package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class GardenCategory {
    private GardenCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.babyzombieaddons.category.garden"))
                .option(bool("pestDisplay", defaults.garden.pestDisplay,
                        () -> config.garden.pestDisplay, v -> config.garden.pestDisplay = v))
                .option(bool("xpOrbSoundRemoval", defaults.garden.xpOrbSoundRemoval,
                        () -> config.garden.xpOrbSoundRemoval, v -> config.garden.xpOrbSoundRemoval = v))
                .option(bool("signAutoRotate", defaults.garden.signAutoRotate,
                        () -> config.garden.signAutoRotate, v -> config.garden.signAutoRotate = v))
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
