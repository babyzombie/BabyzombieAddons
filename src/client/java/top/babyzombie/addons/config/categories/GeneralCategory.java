package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.azureaaron.dandelion.api.controllers.IntegerController;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfig.AutoISDest;
import top.babyzombie.addons.config.ModConfig.KickRecovery;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class GeneralCategory {
    private GeneralCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(Identifier.fromNamespaceAndPath("babyzombieaddons", "config/general"))
                .name(Component.translatable("config.babyzombieaddons.category.general"))
                .option(createBool("autois", defaults.general.autois,
                        () -> config.general.autois, v -> config.general.autois = v))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.autois"))
                        .collapsed(true)
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.autoisDelay"))
                                .description(Component.translatable("config.babyzombieaddons.option.autoisDelay.desc"))
                                .binding(defaults.general.autoisDelay,
                                        () -> config.general.autoisDelay,
                                        v -> config.general.autoisDelay = v)
                                .controller(IntegerController.createBuilder().range(1, 180).slider(1).build())
                                .build())
                        .option(Option.<AutoISDest>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.autoisDest"))
                                .description(Component.translatable("config.babyzombieaddons.option.autoisDest.desc"))
                                .binding(defaults.general.autoisDest,
                                        () -> config.general.autoisDest,
                                        v -> config.general.autoisDest = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.autoisDest." + m.name())))
                                .build())
                        .option(Option.<KickRecovery>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.autoBackToSkyblock"))
                                .description(Component.translatable("config.babyzombieaddons.option.autoBackToSkyblock.desc"))
                                .binding(defaults.general.autoBackToSkyblock,
                                        () -> config.general.autoBackToSkyblock,
                                        v -> config.general.autoBackToSkyblock = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.autoBackToSkyblock." + m.name())))
                                .build())
                        .build())
                .option(createBool("doubleLobby", defaults.general.doubleLobby,
                        () -> config.general.doubleLobby, v -> config.general.doubleLobby = v))
                .option(createBool("autoEnglish", defaults.general.autoEnglish,
                        () -> config.general.autoEnglish, v -> config.general.autoEnglish = v))
                .option(createBool("hideBlockMessages", defaults.general.hideBlockMessages,
                        () -> config.general.hideBlockMessages, v -> config.general.hideBlockMessages = v))
                .option(createBool("crimsonArmorMute", defaults.general.crimsonArmorMute,
                        () -> config.general.crimsonArmorMute, v -> config.general.crimsonArmorMute = v))
                .option(createBool("cancelEnderPearl", defaults.general.cancelEnderPearl,
                        () -> config.general.cancelEnderPearl, v -> config.general.cancelEnderPearl = v))
                .option(createBool("cakeBuffTracker", defaults.general.cakeBuffTracker,
                        () -> config.general.cakeBuffTracker, v -> config.general.cakeBuffTracker = v))
                .option(createBool("itemTimestamp", defaults.general.itemTimestamp,
                        () -> config.general.itemTimestamp, v -> config.general.itemTimestamp = v))
                .option(createBool("betterSignEditing", defaults.general.betterSignEditing,
                        () -> config.general.betterSignEditing, v -> config.general.betterSignEditing = v))
                .option(createBool("quickAuction", defaults.general.quickAuction,
                        () -> config.general.quickAuction, v -> config.general.quickAuction = v))
                .option(createBool("hideClosePlayers", defaults.general.hideClosePlayers,
                        () -> config.general.hideClosePlayers, v -> config.general.hideClosePlayers = v))
                .option(createBool("vanquisherAlert", defaults.general.vanquisherAlert,
                        () -> config.general.vanquisherAlert, v -> config.general.vanquisherAlert = v))
                .option(createBool("autoAbiphoneAnswer", defaults.general.autoAbiphoneAnswer,
                        () -> config.general.autoAbiphoneAnswer, v -> config.general.autoAbiphoneAnswer = v))
                .option(createBool("jerryBoxHelper", defaults.general.jerryBoxHelper,
                        () -> config.general.jerryBoxHelper, v -> config.general.jerryBoxHelper = v))
                .option(createBool("dailyChineseTranslation", defaults.general.dailyChineseTranslation,
                        () -> config.general.dailyChineseTranslation, v -> config.general.dailyChineseTranslation = v))
                .build();
    }

    private static Option<Boolean> createBool(String key, boolean def, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("config.babyzombieaddons.option." + key))
                .description(Component.translatable("config.babyzombieaddons.option." + key + ".desc"))
                .binding(def, getter, setter)
                .controller(ConfigUtils.createBooleanController())
                .build();
    }
}
