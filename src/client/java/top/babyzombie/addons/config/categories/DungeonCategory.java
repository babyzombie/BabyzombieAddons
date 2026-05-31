package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.azureaaron.dandelion.api.controllers.IntegerController;
import net.azureaaron.dandelion.api.controllers.StringController;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfig.RequeueMode;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class DungeonCategory {
    private DungeonCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.babyzombieaddons.category.dungeon"))
                .option(bool("f4CrowdHiding", defaults.dungeon.f4CrowdHiding,
                        () -> config.dungeon.f4CrowdHiding, v -> config.dungeon.f4CrowdHiding = v))
                .option(Option.<RequeueMode>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.autoRequeue"))
                        .description(Component.translatable("config.babyzombieaddons.option.autoRequeue.desc"))
                        .binding(defaults.dungeon.autoRequeue,
                                () -> config.dungeon.autoRequeue,
                                v -> config.dungeon.autoRequeue = v)
                        .controller(ConfigUtils.createEnumController(m ->
                                Component.translatable("config.babyzombieaddons.option.autoRequeue." + m.name())))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.requeue"))
                        .collapsed(true)
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.requeueDelay"))
                                .description(Component.translatable("config.babyzombieaddons.option.requeueDelay.desc"))
                                .binding(defaults.dungeon.requeueDelay,
                                        () -> config.dungeon.requeueDelay,
                                        v -> config.dungeon.requeueDelay = v)
                                .controller(IntegerController.createBuilder().range(0, 300).slider(1).build())
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
                .option(bool("autoChestClose", defaults.dungeon.autoChestClose,
                        () -> config.dungeon.autoChestClose, v -> config.dungeon.autoChestClose = v))
                .option(bool("dailyCounter", defaults.dungeon.dailyCounter,
                        () -> config.dungeon.dailyCounter, v -> config.dungeon.dailyCounter = v))
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
