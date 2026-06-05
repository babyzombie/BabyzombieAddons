package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfig.*;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class SlayerCategory {
    private SlayerCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(net.minecraft.resources.Identifier.fromNamespaceAndPath("babyzombieaddons", "config/slayer"))
                .name(Component.translatable("config.babyzombieaddons.category.slayer"))
                .option(bool("noslayerquest", defaults.slayer.noslayerquest,
                        () -> config.slayer.noslayerquest, v -> config.slayer.noslayerquest = v))
                .option(Option.<EndStoneSwordMode>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.endStoneSwordTimer"))
                        .description(Component.translatable("config.babyzombieaddons.option.endStoneSwordTimer.desc"))
                        .binding(defaults.slayer.endStoneSwordTimer,
                                () -> config.slayer.endStoneSwordTimer,
                                v -> config.slayer.endStoneSwordTimer = v)
                        .controller(ConfigUtils.createEnumController(m ->
                                Component.translatable("config.babyzombieaddons.option.endStoneSwordTimer." + m.name())))
                        .build())
                .option(Option.<RagnarockAxeMode>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.ragnarockAxeTimer"))
                        .description(Component.translatable("config.babyzombieaddons.option.ragnarockAxeTimer.desc"))
                        .binding(defaults.slayer.ragnarockAxeTimer,
                                () -> config.slayer.ragnarockAxeTimer,
                                v -> config.slayer.ragnarockAxeTimer = v)
                        .controller(ConfigUtils.createEnumController(m ->
                                Component.translatable("config.babyzombieaddons.option.ragnarockAxeTimer." + m.name())))
                        .build())
                .option(bool("reaperArmorTimer", defaults.slayer.reaperArmorTimer,
                        () -> config.slayer.reaperArmorTimer, v -> config.slayer.reaperArmorTimer = v))
                .option(Option.<GummyPolarBearMode>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.reheatedGummyPolarBear"))
                        .description(Component.translatable("config.babyzombieaddons.option.reheatedGummyPolarBear.desc"))
                        .binding(defaults.slayer.reheatedGummyPolarBear,
                                () -> config.slayer.reheatedGummyPolarBear,
                                v -> config.slayer.reheatedGummyPolarBear = v)
                        .controller(ConfigUtils.createEnumController(m ->
                                Component.translatable("config.babyzombieaddons.option.reheatedGummyPolarBear." + m.name())))
                        .build())
                .option(bool("pigmanSwordTimer", defaults.slayer.pigmanSwordTimer,
                        () -> config.slayer.pigmanSwordTimer, v -> config.slayer.pigmanSwordTimer = v))
                .option(bool("holyIceTimer", defaults.slayer.holyIceTimer,
                        () -> config.slayer.holyIceTimer, v -> config.slayer.holyIceTimer = v))
                .option(bool("boxLowHPBloodfiend", defaults.slayer.boxLowHPBloodfiend,
                        () -> config.slayer.boxLowHPBloodfiend, v -> config.slayer.boxLowHPBloodfiend = v))
                .option(bool("showEffigies", defaults.slayer.showEffigies,
                        () -> config.slayer.showEffigies, v -> config.slayer.showEffigies = v))
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
