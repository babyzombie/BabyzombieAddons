package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.azureaaron.dandelion.api.controllers.IntegerController;
import net.azureaaron.dandelion.api.controllers.StringController;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfig.HpDisplayMode;
import top.babyzombie.addons.config.ModConfig.RequeueMode;
import top.babyzombie.addons.config.ModConfig.ToxicArrowMinTier;
import top.babyzombie.addons.config.ModConfig.ToxicArrowTiming;
import top.babyzombie.addons.config.ModConfig.TwilightArrowTiming;

import java.awt.Color;
import java.util.function.Supplier;
import java.util.function.Consumer;

public final class KuudraCategory {
    private KuudraCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(net.minecraft.resources.Identifier.fromNamespaceAndPath("babyzombieaddons", "config/kuudra"))
                .name(Component.translatable("config.babyzombieaddons.category.kuudra"))
                .option(Option.<HpDisplayMode>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.hpDisplay"))
                        .description(Component.translatable("config.babyzombieaddons.option.hpDisplay.desc"))
                        .binding(defaults.kuudra.hpDisplay,
                                () -> config.kuudra.hpDisplay,
                                v -> config.kuudra.hpDisplay = v)
                        .controller(ConfigUtils.createEnumController(m ->
                                Component.translatable("config.babyzombieaddons.option.hpDisplay." + m.name())))
                        .build())
                .option(bool("phaseTimer", defaults.kuudra.phaseTimer,
                        () -> config.kuudra.phaseTimer, v -> config.kuudra.phaseTimer = v))
                .option(bool("stunTimer", defaults.kuudra.stunTimer,
                        () -> config.kuudra.stunTimer, v -> config.kuudra.stunTimer = v))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.requeue"))
                        .collapsed(true)
                        .option(Option.<RequeueMode>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.kuudraRequeue"))
                                .description(Component.translatable("config.babyzombieaddons.option.kuudraRequeue.desc"))
                                .binding(defaults.dungeon.kuudraRequeue,
                                        () -> config.dungeon.kuudraRequeue,
                                        v -> config.dungeon.kuudraRequeue = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.requeueMode." + m.name())))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.kuudraRequeueDelay"))
                                .description(Component.translatable("config.babyzombieaddons.option.kuudraRequeueDelay.desc"))
                                .binding(defaults.dungeon.kuudraRequeueDelay,
                                        () -> config.dungeon.kuudraRequeueDelay,
                                        v -> config.dungeon.kuudraRequeueDelay = v)
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
                        .name(Component.translatable("config.babyzombieaddons.group.waypoints"))
                        .option(bool("supplyBeacons", defaults.kuudra.supplyBeacons,
                                () -> config.kuudra.supplyBeacons, v -> config.kuudra.supplyBeacons = v))
                        .option(colour("supplyBeaconColor", defaults.kuudra.supplyBeaconColor,
                                () -> config.kuudra.supplyBeaconColor, v -> config.kuudra.supplyBeaconColor = v))
                        .option(bool("supplyDropoffBeacons", defaults.kuudra.supplyDropoffBeacons,
                                () -> config.kuudra.supplyDropoffBeacons, v -> config.kuudra.supplyDropoffBeacons = v))
                        .option(colour("supplyDropoffBeaconColor", defaults.kuudra.supplyDropoffBeaconColor,
                                () -> config.kuudra.supplyDropoffBeaconColor, v -> config.kuudra.supplyDropoffBeaconColor = v))
                        .option(bool("ballistaProgressText", defaults.kuudra.ballistaProgressText,
                                () -> config.kuudra.ballistaProgressText, v -> config.kuudra.ballistaProgressText = v))
                        .option(colour("ballistaTextColor", defaults.kuudra.ballistaTextColor,
                                () -> config.kuudra.ballistaTextColor, v -> config.kuudra.ballistaTextColor = v))
                        .option(bool("ballistaBuildBeacons", defaults.kuudra.ballistaBuildBeacons,
                                () -> config.kuudra.ballistaBuildBeacons, v -> config.kuudra.ballistaBuildBeacons = v))
                        .option(colour("ballistaBeaconColor", defaults.kuudra.ballistaBeaconColor,
                                () -> config.kuudra.ballistaBeaconColor, v -> config.kuudra.ballistaBeaconColor = v))
                        .option(bool("fuelOrbBeacons", defaults.kuudra.fuelOrbBeacons,
                                () -> config.kuudra.fuelOrbBeacons, v -> config.kuudra.fuelOrbBeacons = v))
                        .option(colour("fuelOrbBeaconColor", defaults.kuudra.fuelOrbBeaconColor,
                                () -> config.kuudra.fuelOrbBeaconColor, v -> config.kuudra.fuelOrbBeaconColor = v))
                        .build())
                .option(bool("energyDisplay", defaults.kuudra.energyDisplay,
                        () -> config.kuudra.energyDisplay, v -> config.kuudra.energyDisplay = v))
                .option(bool("boxKuudra", defaults.kuudra.boxKuudra,
                        () -> config.kuudra.boxKuudra, v -> config.kuudra.boxKuudra = v))
                .option(bool("enderPearlRefill", defaults.kuudra.enderPearlRefill,
                        () -> config.kuudra.enderPearlRefill, v -> config.kuudra.enderPearlRefill = v))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.arrowPoison"))
                        .collapsed(true)
                        .option(Option.<ToxicArrowMinTier>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.toxicArrowMinTier"))
                                .description(Component.translatable("config.babyzombieaddons.option.toxicArrowMinTier.desc"))
                                .binding(defaults.kuudra.toxicArrowMinTier,
                                        () -> config.kuudra.toxicArrowMinTier,
                                        v -> config.kuudra.toxicArrowMinTier = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.toxicArrowMinTier." + m.name())))
                                .build())
                        .option(Option.<ToxicArrowTiming>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.toxicArrowTiming"))
                                .description(Component.translatable("config.babyzombieaddons.option.toxicArrowTiming.desc"))
                                .binding(defaults.kuudra.toxicArrowTiming,
                                        () -> config.kuudra.toxicArrowTiming,
                                        v -> config.kuudra.toxicArrowTiming = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.toxicArrowTiming." + m.name())))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.toxicArrowThreshold"))
                                .description(Component.translatable("config.babyzombieaddons.option.toxicArrowThreshold.desc"))
                                .binding(defaults.kuudra.toxicArrowThreshold,
                                        () -> config.kuudra.toxicArrowThreshold,
                                        v -> config.kuudra.toxicArrowThreshold = v)
                                .controller(IntegerController.createBuilder().range(0, 32).slider(2).build())
                                .build())
                        .option(Option.<TwilightArrowTiming>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.twilightArrowTiming"))
                                .description(Component.translatable("config.babyzombieaddons.option.twilightArrowTiming.desc"))
                                .binding(defaults.kuudra.twilightArrowTiming,
                                        () -> config.kuudra.twilightArrowTiming,
                                        v -> config.kuudra.twilightArrowTiming = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.twilightArrowTiming." + m.name())))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.twilightArrowThreshold"))
                                .description(Component.translatable("config.babyzombieaddons.option.twilightArrowThreshold.desc"))
                                .binding(defaults.kuudra.twilightArrowThreshold,
                                        () -> config.kuudra.twilightArrowThreshold,
                                        v -> config.kuudra.twilightArrowThreshold = v)
                                .controller(IntegerController.createBuilder().range(0, 8).slider(1).build())
                                .build())
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.perkShop"))
                        .collapsed(true)
                        .option(bool("perkShopBlacklist", defaults.kuudra.perkShopBlacklist,
                                () -> config.kuudra.perkShopBlacklist, v -> config.kuudra.perkShopBlacklist = v))
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.perkShopBlacklistItems"))
                                .description(Component.translatable("config.babyzombieaddons.option.perkShopBlacklistItems.desc"))
                                .binding(defaults.kuudra.perkShopBlacklistItems,
                                        () -> config.kuudra.perkShopBlacklistItems,
                                        v -> config.kuudra.perkShopBlacklistItems = v)
                                .controller(StringController.createBuilder().build())
                                .build())
                        .build())
                .option(bool("followerHelmetPrice", defaults.kuudra.followerHelmetPrice,
                        () -> config.kuudra.followerHelmetPrice, v -> config.kuudra.followerHelmetPrice = v))
                .option(bool("muteCrimsonArmor", defaults.kuudra.muteCrimsonArmor,
                        () -> config.kuudra.muteCrimsonArmor, v -> config.kuudra.muteCrimsonArmor = v))
                .option(bool("nopeMagmafish", defaults.kuudra.nopeMagmafish,
                        () -> config.kuudra.nopeMagmafish, v -> config.kuudra.nopeMagmafish = v))
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

    private static Option<Color> colour(String key, int def, Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Color>createBuilder()
                .name(Component.translatable("config.babyzombieaddons.option." + key))
                .description(Component.translatable("config.babyzombieaddons.option." + key + ".desc"))
                .binding(new Color(def, true),
                        () -> new Color(getter.get(), true),
                        v -> setter.accept(v.getRGB()))
                .controller(ConfigUtils.createColourController(true))
                .build();
    }
}
