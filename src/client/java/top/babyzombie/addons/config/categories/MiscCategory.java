package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ButtonOption;
import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.module.raredrop.RareDropScreen;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class MiscCategory {
    private MiscCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.babyzombieaddons.category.misc"))
                .option(bool("killComboHUD", defaults.misc.killComboHUD,
                        () -> config.misc.killComboHUD, v -> config.misc.killComboHUD = v))
                .option(bool("betterPerspective", defaults.misc.betterPerspective,
                        () -> config.misc.betterPerspective, v -> config.misc.betterPerspective = v))
                .option(bool("dukeWaypoint", defaults.misc.dukeWaypoint,
                        () -> config.misc.dukeWaypoint, v -> config.misc.dukeWaypoint = v))
                .option(bool("creeperVisibility", defaults.misc.creeperVisibility,
                        () -> config.misc.creeperVisibility, v -> config.misc.creeperVisibility = v))
                .option(bool("fruitDiggingHelper", defaults.misc.fruitDiggingHelper,
                        () -> config.misc.fruitDiggingHelper, v -> config.misc.fruitDiggingHelper = v))
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
                .option(bool("sackItemHUD", defaults.misc.sackItemHUD,
                        () -> config.misc.sackItemHUD, v -> config.misc.sackItemHUD = v))
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
                .binding(def, getter, setter)
                .controller(ConfigUtils.createBooleanController())
                .build();
    }
}
