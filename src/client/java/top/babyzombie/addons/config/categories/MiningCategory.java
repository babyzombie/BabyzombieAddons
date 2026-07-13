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

public final class MiningCategory {
    private MiningCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(net.minecraft.resources.Identifier.fromNamespaceAndPath("babyzombieaddons", "config/mining"))
                .name(Component.translatable("config.babyzombieaddons.category.mining"))
                .option(bool("miningAbilityAlerts", defaults.mining.miningAbilityAlerts,
                        () -> config.mining.miningAbilityAlerts, v -> config.mining.miningAbilityAlerts = v))
                .option(bool("drillSwingSuppression", defaults.mining.drillSwingSuppression,
                        () -> config.mining.drillSwingSuppression, v -> config.mining.drillSwingSuppression = v))
                .option(bool("creeperVisibility", defaults.mining.creeperVisibility,
                        () -> config.mining.creeperVisibility, v -> config.mining.creeperVisibility = v))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.crystalHollows"))
                        .option(bool("nucleusAutoWarp", defaults.mining.nucleusAutoWarp,
                                () -> config.mining.nucleusAutoWarp, v -> config.mining.nucleusAutoWarp = v))
                        .option(bool("crystalHollowsPassAutoRenew", defaults.mining.crystalHollowsPassAutoRenew,
                                () -> config.mining.crystalHollowsPassAutoRenew, v -> config.mining.crystalHollowsPassAutoRenew = v))
                        .option(bool("chestMarkers", defaults.mining.chestMarkers,
                                () -> config.mining.chestMarkers, v -> config.mining.chestMarkers = v))
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.chestLineWidth"))
                                .description(Component.translatable("config.babyzombieaddons.option.chestLineWidth.desc"))
                                .binding(defaults.mining.chestLineWidth,
                                        () -> config.mining.chestLineWidth,
                                        v -> config.mining.chestLineWidth = v)
                                .controller(IntegerController.createBuilder().range(1, 16).slider(1).build())
                                .build())
                        .option(bool("getFromSacks", defaults.mining.getFromSacks,
                                () -> config.mining.getFromSacks, v -> config.mining.getFromSacks = v))
                        .option(bool("scathaCooldown", defaults.mining.scathaCooldown,
                                () -> config.mining.scathaCooldown, v -> config.mining.scathaCooldown = v))
                        .option(bool("scathaReleaseKey", defaults.mining.scathaReleaseKey,
                                () -> config.mining.scathaReleaseKey, v -> config.mining.scathaReleaseKey = v))
                        .option(bool("armadilloEnergy", defaults.mining.armadilloEnergy,
                                () -> config.mining.armadilloEnergy, v -> config.mining.armadilloEnergy = v))
                        .option(bool("powderMiningSounds", defaults.mining.powderMiningSounds,
                                () -> config.mining.powderMiningSounds, v -> config.mining.powderMiningSounds = v))
                        .option(bool("jungleTempleThinWall", defaults.mining.jungleTempleThinWall,
                                () -> config.mining.jungleTempleThinWall, v -> config.mining.jungleTempleThinWall = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.glaciteTunnels"))
                        .option(bool("mineshaftWaypoints", defaults.mining.mineshaftWaypoints,
                                () -> config.mining.mineshaftWaypoints, v -> config.mining.mineshaftWaypoints = v))
                        .option(Option.<ModConfig.MineshaftWarpMode>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.glaciteMineshaftWarp"))
                                .description(Component.translatable("config.babyzombieaddons.option.glaciteMineshaftWarp.desc"))
                                .binding(defaults.mining.glaciteMineshaftWarp,
                                        () -> config.mining.glaciteMineshaftWarp,
                                        v -> config.mining.glaciteMineshaftWarp = v)
                                .controller(ConfigUtils.createEnumController(b ->
                                        Component.translatable("config.babyzombieaddons.option.glaciteMineshaftWarp." + b.name())))
                                .build())
                        .option(bool("suspiciousScrapCounter", defaults.mining.suspiciousScrapCounter,
                                () -> config.mining.suspiciousScrapCounter, v -> config.mining.suspiciousScrapCounter = v))
                        .option(bool("greatGlaciteWaypoints", defaults.mining.greatGlaciteWaypoints,
                                () -> config.mining.greatGlaciteWaypoints, v -> config.mining.greatGlaciteWaypoints = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.mithrilGourmand"))
                        .option(bool("mithrilGourmandAutoExpresso", defaults.mining.mithrilGourmandAutoExpresso,
                                () -> config.mining.mithrilGourmandAutoExpresso,
                                v -> config.mining.mithrilGourmandAutoExpresso = v))
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.mithrilGourmandTriggerSeconds"))
                                .description(Component.translatable("config.babyzombieaddons.option.mithrilGourmandTriggerSeconds.desc"))
                                .binding(defaults.mining.mithrilGourmandTriggerSeconds,
                                        () -> config.mining.mithrilGourmandTriggerSeconds,
                                        v -> config.mining.mithrilGourmandTriggerSeconds = v)
                                .controller(IntegerController.createBuilder().range(3, 20).slider(1).build())
                                .build())
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
