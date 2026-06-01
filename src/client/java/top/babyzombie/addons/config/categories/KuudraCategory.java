package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class KuudraCategory {
    private KuudraCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(net.minecraft.resources.Identifier.fromNamespaceAndPath("babyzombieaddons", "config/kuudra"))
                .name(Component.translatable("config.babyzombieaddons.category.kuudra"))
                .option(bool("welcomeTitle", defaults.kuudra.welcomeTitle,
                        () -> config.kuudra.welcomeTitle, v -> config.kuudra.welcomeTitle = v))
                .option(bool("hpDisplay", defaults.kuudra.hpDisplay,
                        () -> config.kuudra.hpDisplay, v -> config.kuudra.hpDisplay = v))
                .option(bool("phaseTimer", defaults.kuudra.phaseTimer,
                        () -> config.kuudra.phaseTimer, v -> config.kuudra.phaseTimer = v))
                .option(bool("dropshipWarning", defaults.kuudra.dropshipWarning,
                        () -> config.kuudra.dropshipWarning, v -> config.kuudra.dropshipWarning = v))
                .option(bool("wanderingBlazesWarning", defaults.kuudra.wanderingBlazesWarning,
                        () -> config.kuudra.wanderingBlazesWarning, v -> config.kuudra.wanderingBlazesWarning = v))
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
                .option(bool("perkShopBlacklist", defaults.kuudra.perkShopBlacklist,
                        () -> config.kuudra.perkShopBlacklist, v -> config.kuudra.perkShopBlacklist = v))
                .option(bool("extremeFocusWarning", defaults.kuudra.extremeFocusWarning,
                        () -> config.kuudra.extremeFocusWarning, v -> config.kuudra.extremeFocusWarning = v))
                .option(bool("followerHelmetPrice", defaults.kuudra.followerHelmetPrice,
                        () -> config.kuudra.followerHelmetPrice, v -> config.kuudra.followerHelmetPrice = v))
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
