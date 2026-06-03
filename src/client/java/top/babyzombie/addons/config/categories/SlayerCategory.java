package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
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
                .option(bool("showEffigies", defaults.slayer.showEffigies,
                        () -> config.slayer.showEffigies, v -> config.slayer.showEffigies = v))
                .option(bool("boxLowHPBloodfiend", defaults.slayer.boxLowHPBloodfiend,
                        () -> config.slayer.boxLowHPBloodfiend, v -> config.slayer.boxLowHPBloodfiend = v))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.bossBox"))
                        .collapsed(true)
                        .option(Option.<BoxSlayerMode>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.boxslayerboss"))
                                .description(Component.translatable("config.babyzombieaddons.option.boxslayerboss.desc"))
                                .binding(defaults.slayer.boxslayerboss,
                                        () -> config.slayer.boxslayerboss,
                                        v -> config.slayer.boxslayerboss = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.boxslayerboss." + m.name())))
                                .build())
                        .option(Option.<java.awt.Color>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.boxbosscolor"))
                                .description(Component.translatable("config.babyzombieaddons.option.boxbosscolor.desc"))
                                .binding(defaults.slayer.boxbosscolor,
                                        () -> config.slayer.boxbosscolor,
                                        v -> config.slayer.boxbosscolor = v)
                                .controller(ConfigUtils.createColourController(true))
                                .build())
                        .build())
                .option(bool("bossInfoHUD", defaults.slayer.bossInfoHUD,
                        () -> config.slayer.bossInfoHUD, v -> config.slayer.bossInfoHUD = v))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.bossInfoSettings"))
                        .collapsed(true)
                        .option(Option.<SlayerInfoLevel0_2>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.zombieSlayerInfo"))
                                .description(Component.translatable("config.babyzombieaddons.option.zombieSlayerInfo.desc"))
                                .binding(defaults.slayer.zombieSlayerInfo,
                                        () -> config.slayer.zombieSlayerInfo,
                                        v -> config.slayer.zombieSlayerInfo = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.zombieSlayerInfo." + m.name())))
                                .build())
                        .option(Option.<SlayerInfoLevel0_2>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.spiderSlayerInfo"))
                                .description(Component.translatable("config.babyzombieaddons.option.spiderSlayerInfo.desc"))
                                .binding(defaults.slayer.spiderSlayerInfo,
                                        () -> config.slayer.spiderSlayerInfo,
                                        v -> config.slayer.spiderSlayerInfo = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.spiderSlayerInfo." + m.name())))
                                .build())
                        .option(Option.<SlayerInfoLevel0_2>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.wolfSlayerInfo"))
                                .description(Component.translatable("config.babyzombieaddons.option.wolfSlayerInfo.desc"))
                                .binding(defaults.slayer.wolfSlayerInfo,
                                        () -> config.slayer.wolfSlayerInfo,
                                        v -> config.slayer.wolfSlayerInfo = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.wolfSlayerInfo." + m.name())))
                                .build())
                        .option(Option.<SlayerInfoLevel0_2>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.endermanSlayerInfo"))
                                .description(Component.translatable("config.babyzombieaddons.option.endermanSlayerInfo.desc"))
                                .binding(defaults.slayer.endermanSlayerInfo,
                                        () -> config.slayer.endermanSlayerInfo,
                                        v -> config.slayer.endermanSlayerInfo = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.endermanSlayerInfo." + m.name())))
                                .build())
                        .option(Option.<SlayerInfoLevel0_3>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.blazeSlayerInfo"))
                                .description(Component.translatable("config.babyzombieaddons.option.blazeSlayerInfo.desc"))
                                .binding(defaults.slayer.blazeSlayerInfo,
                                        () -> config.slayer.blazeSlayerInfo,
                                        v -> config.slayer.blazeSlayerInfo = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.blazeSlayerInfo." + m.name())))
                                .build())
                        .option(Option.<SlayerInfoLevel0_2>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.vampireSlayerInfo"))
                                .description(Component.translatable("config.babyzombieaddons.option.vampireSlayerInfo.desc"))
                                .binding(defaults.slayer.vampireSlayerInfo,
                                        () -> config.slayer.vampireSlayerInfo,
                                        v -> config.slayer.vampireSlayerInfo = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.vampireSlayerInfo." + m.name())))
                                .build())
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
