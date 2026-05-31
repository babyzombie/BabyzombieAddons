package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;

public final class GeneralCategory {
    private GeneralCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.babyzombieaddons.category.general"))
                .option(createBool("autoUpdateCheck", defaults.general.autoUpdateCheck,
                        () -> config.general.autoUpdateCheck, v -> config.general.autoUpdateCheck = v))
                .option(createBool("noFog", defaults.general.noFog,
                        () -> config.general.noFog, v -> config.general.noFog = v))
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

    private static Option<Boolean> createBool(String key, boolean def, java.util.function.Supplier<Boolean> getter, java.util.function.Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("config.babyzombieaddons.option." + key))
                .binding(def, getter, setter)
                .controller(ConfigUtils.createBooleanController())
                .build();
    }
}
