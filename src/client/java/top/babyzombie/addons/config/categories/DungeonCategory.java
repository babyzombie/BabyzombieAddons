package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;

public final class DungeonCategory {
    private DungeonCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.babyzombieaddons.category.dungeon"))
                .option(bool("welcomeTitle", defaults.dungeon.welcomeTitle,
                        () -> config.dungeon.welcomeTitle, v -> config.dungeon.welcomeTitle = v))
                .option(bool("bloodReadyAlert", defaults.dungeon.bloodReadyAlert,
                        () -> config.dungeon.bloodReadyAlert, v -> config.dungeon.bloodReadyAlert = v))
                .option(bool("witherKeyMarkers", defaults.dungeon.witherKeyMarkers,
                        () -> config.dungeon.witherKeyMarkers, v -> config.dungeon.witherKeyMarkers = v))
                .option(bool("dupeArcherDetection", defaults.dungeon.dupeArcherDetection,
                        () -> config.dungeon.dupeArcherDetection, v -> config.dungeon.dupeArcherDetection = v))
                .option(bool("boxStarMobs", defaults.dungeon.boxStarMobs,
                        () -> config.dungeon.boxStarMobs, v -> config.dungeon.boxStarMobs = v))
                .option(bool("boxFels", defaults.dungeon.boxFels,
                        () -> config.dungeon.boxFels, v -> config.dungeon.boxFels = v))
                .option(bool("f4CrowdHiding", defaults.dungeon.f4CrowdHiding,
                        () -> config.dungeon.f4CrowdHiding, v -> config.dungeon.f4CrowdHiding = v))
                .option(bool("stormThunderMuting", defaults.dungeon.stormThunderMuting,
                        () -> config.dungeon.stormThunderMuting, v -> config.dungeon.stormThunderMuting = v))
                .option(bool("autoChestClose", defaults.dungeon.autoChestClose,
                        () -> config.dungeon.autoChestClose, v -> config.dungeon.autoChestClose = v))
                .option(bool("noAligned", defaults.dungeon.noAligned,
                        () -> config.dungeon.noAligned, v -> config.dungeon.noAligned = v))
                .option(bool("dailyCounter", defaults.dungeon.dailyCounter,
                        () -> config.dungeon.dailyCounter, v -> config.dungeon.dailyCounter = v))
                .option(bool("autoRequeue", defaults.dungeon.autoRequeue,
                        () -> config.dungeon.autoRequeue, v -> config.dungeon.autoRequeue = v))
                .option(bool("instanceWarp", defaults.dungeon.instanceWarp,
                        () -> config.dungeon.instanceWarp, v -> config.dungeon.instanceWarp = v))
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.readyCheckDelay"))
                        .description(Component.translatable("config.babyzombieaddons.option.readyCheckDelay.desc"))
                        .binding(defaults.dungeon.readyCheckDelay,
                                () -> config.dungeon.readyCheckDelay,
                                v -> config.dungeon.readyCheckDelay = v)
                        .controller(net.azureaaron.dandelion.api.controllers.IntegerController.createBuilder().range(1, 30).slider(1).build())
                        .build())
                .build();
    }

    private static Option<Boolean> bool(String key, boolean def, java.util.function.Supplier<Boolean> getter, java.util.function.Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("config.babyzombieaddons.option." + key))
                .description(Component.translatable("config.babyzombieaddons.option." + key + ".desc"))
                .binding(def, getter, setter)
                .controller(ConfigUtils.createBooleanController())
                .build();
    }
}
