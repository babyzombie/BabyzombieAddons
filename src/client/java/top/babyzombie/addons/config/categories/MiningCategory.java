package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class MiningCategory {
    private MiningCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.babyzombieaddons.category.mining"))
                .option(bool("nucleusAutoWarp", defaults.mining.nucleusAutoWarp,
                        () -> config.mining.nucleusAutoWarp, v -> config.mining.nucleusAutoWarp = v))
                .option(bool("miningAbilityAlerts", defaults.mining.miningAbilityAlerts,
                        () -> config.mining.miningAbilityAlerts, v -> config.mining.miningAbilityAlerts = v))
                .option(bool("crystalHollowsPassAutoRenew", defaults.mining.crystalHollowsPassAutoRenew,
                        () -> config.mining.crystalHollowsPassAutoRenew, v -> config.mining.crystalHollowsPassAutoRenew = v))
                .option(bool("chestMarkers", defaults.mining.chestMarkers,
                        () -> config.mining.chestMarkers, v -> config.mining.chestMarkers = v))
                .option(bool("getFromSacks", defaults.mining.getFromSacks,
                        () -> config.mining.getFromSacks, v -> config.mining.getFromSacks = v))
                .option(bool("scathaCooldown", defaults.mining.scathaCooldown,
                        () -> config.mining.scathaCooldown, v -> config.mining.scathaCooldown = v))
                .option(bool("armadilloEnergy", defaults.mining.armadilloEnergy,
                        () -> config.mining.armadilloEnergy, v -> config.mining.armadilloEnergy = v))
                .option(bool("compassSolver", defaults.mining.compassSolver,
                        () -> config.mining.compassSolver, v -> config.mining.compassSolver = v))
                .option(bool("darkMonolithFinder", defaults.mining.darkMonolithFinder,
                        () -> config.mining.darkMonolithFinder, v -> config.mining.darkMonolithFinder = v))
                .option(bool("drillSwingSuppression", defaults.mining.drillSwingSuppression,
                        () -> config.mining.drillSwingSuppression, v -> config.mining.drillSwingSuppression = v))
                .option(bool("powderMiningSounds", defaults.mining.powderMiningSounds,
                        () -> config.mining.powderMiningSounds, v -> config.mining.powderMiningSounds = v))
                .option(bool("noPickobulus", defaults.mining.noPickobulus,
                        () -> config.mining.noPickobulus, v -> config.mining.noPickobulus = v))
                .option(bool("glaciteWaypoints", defaults.mining.glaciteWaypoints,
                        () -> config.mining.glaciteWaypoints, v -> config.mining.glaciteWaypoints = v))
                .option(bool("mineshaftWaypoints", defaults.mining.mineshaftWaypoints,
                        () -> config.mining.mineshaftWaypoints, v -> config.mining.mineshaftWaypoints = v))
                .option(bool("suspiciousScrapCounter", defaults.mining.suspiciousScrapCounter,
                        () -> config.mining.suspiciousScrapCounter, v -> config.mining.suspiciousScrapCounter = v))
                .option(bool("baseCampQuickWarp", defaults.mining.baseCampQuickWarp,
                        () -> config.mining.baseCampQuickWarp, v -> config.mining.baseCampQuickWarp = v))
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
