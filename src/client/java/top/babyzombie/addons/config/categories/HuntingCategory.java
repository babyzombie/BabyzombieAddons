package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class HuntingCategory {
    private HuntingCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(Identifier.fromNamespaceAndPath("babyzombieaddons", "config/hunting"))
                .name(Component.translatable("config.babyzombieaddons.category.hunting"))

                // ── Safari ──
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.safari"))
                        .option(bool("safariBellDisplay", defaults.hunting.safariBellDisplay,
                                () -> config.hunting.safariBellDisplay,
                                v -> config.hunting.safariBellDisplay = v))
                        .option(bool("safariShulkerGlow", defaults.hunting.safariShulkerGlow,
                                () -> config.hunting.safariShulkerGlow,
                                v -> config.hunting.safariShulkerGlow = v))
                        .option(bool("safariHideyhoGlow", defaults.hunting.safariHideyhoGlow,
                                () -> config.hunting.safariHideyhoGlow,
                                v -> config.hunting.safariHideyhoGlow = v))
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
