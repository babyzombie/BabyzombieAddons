package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
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
                        .option(bool("fruitDiggingAutoAccept", defaults.events.fruitDiggingAutoAccept,
                                () -> config.events.fruitDiggingAutoAccept, v -> config.events.fruitDiggingAutoAccept = v))
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
