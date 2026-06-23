package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.azureaaron.dandelion.api.controllers.IntegerController;
import net.azureaaron.dandelion.api.controllers.StringController;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfig.AutoPotionsMode;
import top.babyzombie.addons.config.ModConfig.CrowdHideMode;
import top.babyzombie.addons.config.ModConfig.DailyCounterMode;
import top.babyzombie.addons.config.ModConfig.DeathMessageAction;
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
                .option(Option.<DeathMessageAction>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.deathMessageAction"))
                        .description(Component.translatable("config.babyzombieaddons.option.deathMessageAction.desc"))
                        .binding(defaults.dungeon.deathMessageAction,
                                () -> config.dungeon.deathMessageAction,
                                v -> config.dungeon.deathMessageAction = v)
                        .controller(ConfigUtils.createEnumController(m ->
                                Component.translatable("config.babyzombieaddons.option.deathMessageAction." + m.name())))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.requeue"))
                        .collapsed(true)
                        .option(Option.<RequeueMode>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.dungeonRequeue"))
                                .description(Component.translatable("config.babyzombieaddons.option.dungeonRequeue.desc"))
                                .binding(defaults.dungeon.dungeonRequeue,
                                        () -> config.dungeon.dungeonRequeue,
                                        v -> config.dungeon.dungeonRequeue = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.requeueMode." + m.name())))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.dungeonRequeueDelay"))
                                .description(Component.translatable("config.babyzombieaddons.option.dungeonRequeueDelay.desc"))
                                .binding(defaults.dungeon.dungeonRequeueDelay,
                                        () -> config.dungeon.dungeonRequeueDelay,
                                        v -> config.dungeon.dungeonRequeueDelay = v)
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
                        .option(bool("hideChargedCreepers", defaults.witherCloak.hideChargedCreepers,
                                () -> config.witherCloak.hideChargedCreepers, v -> config.witherCloak.hideChargedCreepers = v))
                        .build())
                .option(bool("dungeonEnderPearlRefill", defaults.dungeon.enderPearlRefill,
                        () -> config.dungeon.enderPearlRefill, v -> config.dungeon.enderPearlRefill = v))
                .option(bool("autoChestClose", defaults.dungeon.autoChestClose,
                        () -> config.dungeon.autoChestClose, v -> config.dungeon.autoChestClose = v))
                .option(bool("muteStormThunder", defaults.dungeon.muteStormThunder,
                        () -> config.dungeon.muteStormThunder, v -> config.dungeon.muteStormThunder = v))
                .option(Option.<AutoPotionsMode>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.autoOpenPotions"))
                        .description(Component.translatable("config.babyzombieaddons.option.autoOpenPotions.desc"))
                        .binding(defaults.dungeon.autoOpenPotions,
                                () -> config.dungeon.autoOpenPotions,
                                v -> config.dungeon.autoOpenPotions = v)
                        .controller(ConfigUtils.createEnumController(m ->
                                Component.translatable("config.babyzombieaddons.option.autoOpenPotions." + m.name())))
                        .build())
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
