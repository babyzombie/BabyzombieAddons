package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class SlayerCategory {
    private SlayerCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(net.minecraft.resources.Identifier.fromNamespaceAndPath("babyzombieaddons", "config/slayer"))
                .name(Component.translatable("config.babyzombieaddons.category.slayer"))
                .option(bool("pigmanSwordTimer", defaults.slayer.pigmanSwordTimer,
                        () -> config.slayer.pigmanSwordTimer, v -> config.slayer.pigmanSwordTimer = v))
                .option(bool("ragnarockAxeTimer", defaults.slayer.ragnarockAxeTimer,
                        () -> config.slayer.ragnarockAxeTimer, v -> config.slayer.ragnarockAxeTimer = v))
                .option(bool("reaperArmorTimer", defaults.slayer.reaperArmorTimer,
                        () -> config.slayer.reaperArmorTimer, v -> config.slayer.reaperArmorTimer = v))
                .option(bool("endStoneSwordTimer", defaults.slayer.endStoneSwordTimer,
                        () -> config.slayer.endStoneSwordTimer, v -> config.slayer.endStoneSwordTimer = v))
                .option(bool("bossInfoHUD", defaults.slayer.bossInfoHUD,
                        () -> config.slayer.bossInfoHUD, v -> config.slayer.bossInfoHUD = v))
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
