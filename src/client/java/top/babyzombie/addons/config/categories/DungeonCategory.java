package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.azureaaron.dandelion.api.controllers.IntegerController;
import net.azureaaron.dandelion.api.controllers.StringController;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfig.CrowdHideMode;
import top.babyzombie.addons.config.ModConfig.DailyCounterMode;
import top.babyzombie.addons.config.ModConfig.RequeueMode;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class DungeonCategory {
    private DungeonCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(net.minecraft.resources.Identifier.fromNamespaceAndPath("babyzombieaddons", "config/dungeon"))
                .name(Component.translatable("config.babyzombieaddons.category.dungeon"))
                .option(Option.<CrowdHideMode>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.f4CrowdHiding"))
                        .description(Component.translatable("config.babyzombieaddons.option.f4CrowdHiding.desc"))
                        .binding(defaults.dungeon.f4CrowdHiding,
                                () -> config.dungeon.f4CrowdHiding,
                                v -> config.dungeon.f4CrowdHiding = v)
                        .controller(ConfigUtils.createEnumController(m ->
                                Component.translatable("config.babyzombieaddons.option.f4CrowdHiding." + m.name())))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.requeue"))
                        .collapsed(true)
                        .option(Option.<RequeueMode>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.autoRequeue"))
                                .description(Component.translatable("config.babyzombieaddons.option.autoRequeue.desc"))
                                .binding(defaults.dungeon.autoRequeue,
                                        () -> config.dungeon.autoRequeue,
                                        v -> config.dungeon.autoRequeue = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.autoRequeue." + m.name())))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.requeueDelay"))
                                .description(Component.translatable("config.babyzombieaddons.option.requeueDelay.desc"))
                                .binding(defaults.dungeon.requeueDelay,
                                        () -> config.dungeon.requeueDelay,
                                        v -> config.dungeon.requeueDelay = v)
                                .controller(IntegerController.createBuilder().range(0, 60).slider(1).build())
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.requeueMessage"))
                                .description(Component.translatable("config.babyzombieaddons.option.requeueMessage.desc"))
                                .binding(defaults.dungeon.requeueMessage,
                                        () -> config.dungeon.requeueMessage,
                                        v -> config.dungeon.requeueMessage = v)
                                .controller(StringController.createBuilder().build())
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.requeueCancelMessage"))
                                .description(Component.translatable("config.babyzombieaddons.option.requeueCancelMessage.desc"))
                                .binding(defaults.dungeon.requeueCancelMessage,
                                        () -> config.dungeon.requeueCancelMessage,
                                        v -> config.dungeon.requeueCancelMessage = v)
                                .controller(StringController.createBuilder().build())
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.requeueCancelKeywords"))
                                .description(Component.translatable("config.babyzombieaddons.option.requeueCancelKeywords.desc"))
                                .binding(defaults.dungeon.requeueCancelKeywords,
                                        () -> config.dungeon.requeueCancelKeywords,
                                        v -> config.dungeon.requeueCancelKeywords = v)
                                .controller(StringController.createBuilder().build())
                                .build())
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.witherCloak"))
                        .collapsed(true)
                        .option(bool("witherCloakTimer", defaults.witherCloak.witherCloakTimer,
                                () -> config.witherCloak.witherCloakTimer, v -> config.witherCloak.witherCloakTimer = v))
                        .option(bool("soulwardTimer", defaults.witherCloak.soulwardTimer,
                                () -> config.witherCloak.soulwardTimer, v -> config.witherCloak.soulwardTimer = v))
                        .option(bool("alignedTimer", defaults.witherCloak.alignedTimer,
                                () -> config.witherCloak.alignedTimer, v -> config.witherCloak.alignedTimer = v))
                        .option(bool("gravityStormTimer", defaults.witherCloak.gravityStormTimer,
                                () -> config.witherCloak.gravityStormTimer, v -> config.witherCloak.gravityStormTimer = v))
                        .build())
                .option(bool("autoChestClose", defaults.dungeon.autoChestClose,
                        () -> config.dungeon.autoChestClose, v -> config.dungeon.autoChestClose = v))
                .option(Option.<DailyCounterMode>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.dailyRunsCounter"))
                        .description(Component.translatable("config.babyzombieaddons.option.dailyRunsCounter.desc"))
                        .binding(defaults.dungeon.dailyRunsCounter,
                                () -> config.dungeon.dailyRunsCounter,
                                v -> config.dungeon.dailyRunsCounter = v)
                        .controller(ConfigUtils.createEnumController(m ->
                                Component.translatable("config.babyzombieaddons.option.dailyRunsCounter." + m.name())))
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
