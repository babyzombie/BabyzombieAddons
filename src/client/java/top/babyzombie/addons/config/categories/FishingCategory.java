package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class FishingCategory {
    private FishingCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.babyzombieaddons.category.fishing"))
                .option(bool("legendaryAlerts", defaults.fishing.legendaryAlerts,
                        () -> config.fishing.legendaryAlerts, v -> config.fishing.legendaryAlerts = v))
                .option(bool("volcanoSteamReduction", defaults.fishing.volcanoSteamReduction,
                        () -> config.fishing.volcanoSteamReduction, v -> config.fishing.volcanoSteamReduction = v))
                .option(bool("slugfishHookLock", defaults.fishing.slugfishHookLock,
                        () -> config.fishing.slugfishHookLock, v -> config.fishing.slugfishHookLock = v))
                .option(bool("killInvisibleGoldenFish", defaults.fishing.killInvisibleGoldenFish,
                        () -> config.fishing.killInvisibleGoldenFish, v -> config.fishing.killInvisibleGoldenFish = v))
                .option(bool("reindrakeHP", defaults.fishing.reindrakeHP,
                        () -> config.fishing.reindrakeHP, v -> config.fishing.reindrakeHP = v))
                .build();
    }

    private static Option<Boolean> bool(String key, boolean def, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("config.babyzombieaddons.option." + key))
                .binding(def, getter, setter)
                .controller(ConfigUtils.createBooleanController())
                .build();
    }
}
