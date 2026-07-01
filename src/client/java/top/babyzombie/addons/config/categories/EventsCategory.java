package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.azureaaron.dandelion.api.controllers.FloatController;
import net.azureaaron.dandelion.api.controllers.IntegerController;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class EventsCategory {
    private EventsCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(net.minecraft.resources.Identifier.fromNamespaceAndPath("babyzombieaddons", "config/events"))
                .name(Component.translatable("config.babyzombieaddons.category.events"))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.greatSpook"))
                        .option(bool("greatSpook", defaults.events.greatSpook,
                                () -> config.events.greatSpook, v -> config.events.greatSpook = v))
                        .option(bool("greatSpookDelay", defaults.events.greatSpookDelay,
                                () -> config.events.greatSpookDelay, v -> config.events.greatSpookDelay = v))
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.publicSpeakingDemon"))
                                .description(Component.translatable("config.babyzombieaddons.option.publicSpeakingDemon.desc"))
                                .binding(defaults.events.publicSpeakingDemon,
                                        () -> config.events.publicSpeakingDemon,
                                        v -> config.events.publicSpeakingDemon = v)
                                .controller(ConfigUtils.createStringController())
                                .build())
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.carnival"))
                        .option(bool("fruitDiggingHelper", defaults.events.fruitDiggingHelper,
                                () -> config.events.fruitDiggingHelper, v -> config.events.fruitDiggingHelper = v))
                        .option(bool("carnivalAutoAccept", defaults.events.carnivalAutoAccept,
                                () -> config.events.carnivalAutoAccept, v -> config.events.carnivalAutoAccept = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.fruitDiggingSolver"))
                        .option(bool("fruitDiggingSolver", defaults.events.fruitDiggingSolver,
                                () -> config.events.fruitDiggingSolver, v -> config.events.fruitDiggingSolver = v))
                        .option(createFloat("solverBombPenalty", 0, 1000, defaults.events.solverBombPenalty,
                                () -> config.events.solverBombPenalty, v -> config.events.solverBombPenalty = v))
                        .option(createFloat("solverRumPenalty", 0, 1000, defaults.events.solverRumPenalty,
                                () -> config.events.solverRumPenalty, v -> config.events.solverRumPenalty = v))
                        .option(createFloat("solverMinesInfoWeight", 0, 200, defaults.events.solverMinesInfoWeight,
                                () -> config.events.solverMinesInfoWeight, v -> config.events.solverMinesInfoWeight = v))
                        .option(createFloat("solverTreasureInfoWeight", 0, 200, defaults.events.solverTreasureInfoWeight,
                                () -> config.events.solverTreasureInfoWeight, v -> config.events.solverTreasureInfoWeight = v))
                        .option(createFloat("solverAnchorInfoWeight", 0, 200, defaults.events.solverAnchorInfoWeight,
                                () -> config.events.solverAnchorInfoWeight, v -> config.events.solverAnchorInfoWeight = v))
                        .option(createFloat("solverEarlyAppleBonus", 0, 500, defaults.events.solverEarlyAppleBonus,
                                () -> config.events.solverEarlyAppleBonus, v -> config.events.solverEarlyAppleBonus = v))
                        .option(createFloat("solverEarlyCherryBonus", 0, 500, defaults.events.solverEarlyCherryBonus,
                                () -> config.events.solverEarlyCherryBonus, v -> config.events.solverEarlyCherryBonus = v))
                        .option(createInt("solverLateGameDigs", 0, 15, defaults.events.solverLateGameDigs,
                                () -> config.events.solverLateGameDigs, v -> config.events.solverLateGameDigs = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.anniversary"))
                        .option(bool("raffleTaskTracker", defaults.events.raffleTaskTracker,
                                () -> config.events.raffleTaskTracker, v -> config.events.raffleTaskTracker = v))
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

    private static Option<Float> createFloat(String key, float min, float max, float def,
                                              Supplier<Float> getter, Consumer<Float> setter) {
        return Option.<Float>createBuilder()
                .name(Component.translatable("config.babyzombieaddons.option." + key))
                .description(Component.translatable("config.babyzombieaddons.option." + key + ".desc"))
                .binding(def, getter, setter)
                .controller(FloatController.createBuilder().range(min, max).build())
                .build();
    }

    private static Option<Integer> createInt(String key, int min, int max, int def,
                                              Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Integer>createBuilder()
                .name(Component.translatable("config.babyzombieaddons.option." + key))
                .description(Component.translatable("config.babyzombieaddons.option." + key + ".desc"))
                .binding(def, getter, setter)
                .controller(IntegerController.createBuilder().range(min, max).build())
                .build();
    }
}
