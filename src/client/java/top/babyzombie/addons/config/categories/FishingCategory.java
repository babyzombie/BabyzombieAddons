package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class FishingCategory {
    private FishingCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(Identifier.fromNamespaceAndPath("babyzombieaddons", "config/fishing"))
                .name(Component.translatable("config.babyzombieaddons.category.fishing"))

                .option(bool("rareSeaCreaturesAlert", defaults.fishing.rareSeaCreaturesAlert,
                        () -> config.fishing.rareSeaCreaturesAlert,
                        v -> config.fishing.rareSeaCreaturesAlert = v))
                .option(bool("rareSeaCreaturesAlertTitle", defaults.fishing.rareSeaCreaturesAlertTitle,
                        () -> config.fishing.rareSeaCreaturesAlertTitle,
                        v -> config.fishing.rareSeaCreaturesAlertTitle = v))

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
