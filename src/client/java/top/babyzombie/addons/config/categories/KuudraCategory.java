package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.azureaaron.dandelion.api.controllers.StringController;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfig.HpDisplayMode;

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
                .option(bool("waypoints", defaults.kuudra.waypoints,
                        () -> config.kuudra.waypoints, v -> config.kuudra.waypoints = v))
                .option(bool("energyDisplay", defaults.kuudra.energyDisplay,
                        () -> config.kuudra.energyDisplay, v -> config.kuudra.energyDisplay = v))
                .option(bool("directionIndicator", defaults.kuudra.directionIndicator,
                        () -> config.kuudra.directionIndicator, v -> config.kuudra.directionIndicator = v))
                .option(bool("boxKuudra", defaults.kuudra.boxKuudra,
                        () -> config.kuudra.boxKuudra, v -> config.kuudra.boxKuudra = v))
                .option(bool("enderPearlRefill", defaults.kuudra.enderPearlRefill,
                        () -> config.kuudra.enderPearlRefill, v -> config.kuudra.enderPearlRefill = v))
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
