package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.azureaaron.dandelion.api.controllers.IntegerController;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class GardenCategory {
    private GardenCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(net.minecraft.resources.Identifier.fromNamespaceAndPath("babyzombieaddons", "config/garden"))
                .name(Component.translatable("config.babyzombieaddons.category.garden"))
                .option(bool("pestDisplay", defaults.garden.pestDisplay,
                        () -> config.garden.pestDisplay, v -> config.garden.pestDisplay = v))
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.xpOrbSoundRemoval"))
                        .description(Component.translatable("config.babyzombieaddons.option.xpOrbSoundRemoval.desc"))
                        .binding(defaults.garden.xpOrbSoundRemoval,
                                () -> config.garden.xpOrbSoundRemoval,
                                v -> config.garden.xpOrbSoundRemoval = v)
                        .controller(IntegerController.createBuilder().range(0, 100).slider(1).build())
                        .build())
                .option(bool("signAutoRotate", defaults.garden.signAutoRotate,
                        () -> config.garden.signAutoRotate, v -> config.garden.signAutoRotate = v))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.trevor"))
                        .option(bool("trevorAutoAccept", defaults.garden.trevorAutoAccept,
                                () -> config.garden.trevorAutoAccept, v -> config.garden.trevorAutoAccept = v))
                        .option(bool("trevorAutoCall", defaults.garden.trevorAutoCall,
                                () -> config.garden.trevorAutoCall, v -> config.garden.trevorAutoCall = v))
                        .build())
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
