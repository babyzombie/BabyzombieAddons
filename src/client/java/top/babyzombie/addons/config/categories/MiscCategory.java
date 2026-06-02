package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ButtonOption;
import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.HudManager;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfig.ConfigBackend;
import top.babyzombie.addons.module.raredrop.RareDropScreen;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class MiscCategory {
    private MiscCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(net.minecraft.resources.Identifier.fromNamespaceAndPath("babyzombieaddons", "config/misc"))
                .name(Component.translatable("config.babyzombieaddons.category.misc"))
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.hudEdit"))
                        .description(Component.translatable("config.babyzombieaddons.option.hudEdit.desc"))
                        .action(screen -> HudManager.toggleEditMode())
                        .build())
                .option(bool("debugMode", defaults.debug.debugMode,
                        () -> config.debug.debugMode, v -> config.debug.debugMode = v))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.configBackend"))
                        .collapsed(true)
                        .option(Option.<ConfigBackend>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.configBackend"))
                                .description(Component.translatable("config.babyzombieaddons.option.configBackend.desc"))
                                .binding(defaults.debug.configBackend,
                                        () -> config.debug.configBackend,
                                        v -> config.debug.configBackend = v)
                                .controller(ConfigUtils.createEnumController(b -> switch (b) {
                                    case YACL -> Component.literal("YACL");
                                    case MOUL_CONFIG -> Component.literal("MoulConfig");
                                }))
                                .build())
                        .build())
                .option(bool("killComboHUD", defaults.misc.killComboHUD,
                        () -> config.misc.killComboHUD, v -> config.misc.killComboHUD = v))
                .option(bool("betterPerspective", defaults.misc.betterPerspective,
                        () -> config.misc.betterPerspective, v -> config.misc.betterPerspective = v))
                .option(bool("dukeWaypoint", defaults.misc.dukeWaypoint,
                        () -> config.misc.dukeWaypoint, v -> config.misc.dukeWaypoint = v))
                .option(bool("creeperVisibility", defaults.misc.creeperVisibility,
                        () -> config.misc.creeperVisibility, v -> config.misc.creeperVisibility = v))
                .option(bool("bazaarSackExtraction", defaults.misc.bazaarSackExtraction,
                        () -> config.misc.bazaarSackExtraction, v -> config.misc.bazaarSackExtraction = v))
                .option(bool("personalCompactorPreview", defaults.misc.personalCompactorPreview,
                        () -> config.misc.personalCompactorPreview, v -> config.misc.personalCompactorPreview = v))
                .option(bool("hideHyperionExplosion", defaults.misc.hideHyperionExplosion,
                        () -> config.misc.hideHyperionExplosion, v -> config.misc.hideHyperionExplosion = v))
                .option(bool("witherImpactVolume", defaults.misc.witherImpactVolume,
                        () -> config.misc.witherImpactVolume, v -> config.misc.witherImpactVolume = v))
                .option(bool("dyedArmorSBID", defaults.misc.dyedArmorSBID,
                        () -> config.misc.dyedArmorSBID, v -> config.misc.dyedArmorSBID = v))

                .option(bool("periodicEntityCleanup", defaults.misc.periodicEntityCleanup,
                        () -> config.misc.periodicEntityCleanup, v -> config.misc.periodicEntityCleanup = v))
                .option(bool("stonksPrice", defaults.misc.stonksPrice,
                        () -> config.misc.stonksPrice, v -> config.misc.stonksPrice = v))
                .option(bool("bitsShopPrice", defaults.misc.bitsShopPrice,
                        () -> config.misc.bitsShopPrice, v -> config.misc.bitsShopPrice = v))
                .option(bool("attributeDisplay", defaults.misc.attributeDisplay,
                        () -> config.misc.attributeDisplay, v -> config.misc.attributeDisplay = v))
                .option(bool("timeChime", defaults.misc.timeChime,
                        () -> config.misc.timeChime, v -> config.misc.timeChime = v))
                .option(bool("abiphoneGui", defaults.misc.abiphoneGui,
                        () -> config.misc.abiphoneGui, v -> config.misc.abiphoneGui = v))
                .option(bool("playCmd", defaults.misc.playCmd,
                        () -> config.misc.playCmd, v -> config.misc.playCmd = v))
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.raredropManage"))
                        .action(screen -> Minecraft.getInstance().setScreen(new RareDropScreen(screen)))
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
