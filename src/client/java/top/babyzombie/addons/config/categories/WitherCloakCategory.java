package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class WitherCloakCategory {
    private WitherCloakCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.babyzombieaddons.category.witherCloak"))
                .option(bool("witherCloakTimer", defaults.witherCloak.witherCloakTimer,
                        () -> config.witherCloak.witherCloakTimer, v -> config.witherCloak.witherCloakTimer = v))
                .option(bool("soulwardTimer", defaults.witherCloak.soulwardTimer,
                        () -> config.witherCloak.soulwardTimer, v -> config.witherCloak.soulwardTimer = v))
                .option(bool("alignedTimer", defaults.witherCloak.alignedTimer,
                        () -> config.witherCloak.alignedTimer, v -> config.witherCloak.alignedTimer = v))
                .option(bool("gravityStormTimer", defaults.witherCloak.gravityStormTimer,
                        () -> config.witherCloak.gravityStormTimer, v -> config.witherCloak.gravityStormTimer = v))
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
