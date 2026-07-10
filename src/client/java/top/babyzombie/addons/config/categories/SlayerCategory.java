package top.babyzombie.addons.config.categories;

import java.awt.Color;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.azureaaron.dandelion.api.controllers.IntegerController;
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

                // ── Item Skill Timers ──
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.itemSkillTimers"))
                        .option(bool("pigmanSwordTimer", defaults.slayer.pigmanSwordTimer,
                                () -> config.slayer.pigmanSwordTimer, v -> config.slayer.pigmanSwordTimer = v))
                        .option(bool("holyIceTimer", defaults.slayer.holyIceTimer,
                                () -> config.slayer.holyIceTimer, v -> config.slayer.holyIceTimer = v))
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
                        .option(Option.<EndStoneSwordMode>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.endStoneSwordTimer"))
                                .description(Component.translatable("config.babyzombieaddons.option.endStoneSwordTimer.desc"))
                                .binding(defaults.slayer.endStoneSwordTimer,
                                        () -> config.slayer.endStoneSwordTimer,
                                        v -> config.slayer.endStoneSwordTimer = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.endStoneSwordTimer." + m.name())))
                                .build())
                        .option(Option.<GummyPolarBearMode>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.reheatedGummyPolarBear"))
                                .description(Component.translatable("config.babyzombieaddons.option.reheatedGummyPolarBear.desc"))
                                .binding(defaults.slayer.reheatedGummyPolarBear,
                                        () -> config.slayer.reheatedGummyPolarBear,
                                        v -> config.slayer.reheatedGummyPolarBear = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.reheatedGummyPolarBear." + m.name())))
                                .build())
                        .build())

                // ── Boss Info ──
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.slayerBossInfo"))
                        .option(bossInfo("zombieSlayerInfo", defaults.slayer.zombieSlayerInfo,
                                () -> config.slayer.zombieSlayerInfo, v -> config.slayer.zombieSlayerInfo = v))
                        .option(bossInfo("spiderSlayerInfo", defaults.slayer.spiderSlayerInfo,
                                () -> config.slayer.spiderSlayerInfo, v -> config.slayer.spiderSlayerInfo = v))
                        .option(bossInfo("wolfSlayerInfo", defaults.slayer.wolfSlayerInfo,
                                () -> config.slayer.wolfSlayerInfo, v -> config.slayer.wolfSlayerInfo = v))
                        .option(bossInfo("endermanSlayerInfo", defaults.slayer.endermanSlayerInfo,
                                () -> config.slayer.endermanSlayerInfo, v -> config.slayer.endermanSlayerInfo = v))
                        .option(bossInfo("blazeSlayerInfo", defaults.slayer.blazeSlayerInfo,
                                () -> config.slayer.blazeSlayerInfo, v -> config.slayer.blazeSlayerInfo = v))
                        .option(bossInfo("vampireSlayerInfo", defaults.slayer.vampireSlayerInfo,
                                () -> config.slayer.vampireSlayerInfo, v -> config.slayer.vampireSlayerInfo = v))
                        .build())

                // ── Boss Box ──
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.slayerBossBox"))
                        .option(Option.<SlayerBossBoxMode>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.boxSlayerBoss"))
                                .description(Component.translatable("config.babyzombieaddons.option.boxSlayerBoss.desc"))
                                .binding(defaults.slayer.boxSlayerBoss,
                                        () -> config.slayer.boxSlayerBoss,
                                        v -> config.slayer.boxSlayerBoss = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.boxSlayerBoss." + m.name())))
                                .build())
                        .option(bool("boxBossRenderThroughWalls", defaults.slayer.boxBossRenderThroughWalls,
                                () -> config.slayer.boxBossRenderThroughWalls,
                                v -> config.slayer.boxBossRenderThroughWalls = v))
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.boxBossLineWidth"))
                                .description(Component.translatable("config.babyzombieaddons.option.boxBossLineWidth.desc"))
                                .binding(defaults.slayer.boxBossLineWidth,
                                        () -> config.slayer.boxBossLineWidth,
                                        v -> config.slayer.boxBossLineWidth = v)
                                .controller(IntegerController.createBuilder().range(1, 16).slider(1).build())
                                .build())
                        .option(Option.<Color>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.boxBossColor"))
                                .description(Component.translatable("config.babyzombieaddons.option.boxBossColor.desc"))
                                .binding(new Color(defaults.slayer.boxBossColor, true),
                                        () -> new Color(config.slayer.boxBossColor, true),
                                        v -> config.slayer.boxBossColor = v.getRGB())
                                .controller(ConfigUtils.createColourController(true))
                                .build())
                        .option(bool("boxBossBeam", defaults.slayer.boxBossBeam,
                                () -> config.slayer.boxBossBeam, v -> config.slayer.boxBossBeam = v))
                        .option(Option.<Color>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.boxBossBeamColor"))
                                .description(Component.translatable("config.babyzombieaddons.option.boxBossBeamColor.desc"))
                                .binding(new Color(defaults.slayer.boxBossBeamColor, true),
                                        () -> new Color(config.slayer.boxBossBeamColor, true),
                                        v -> config.slayer.boxBossBeamColor = v.getRGB())
                                .controller(ConfigUtils.createColourController(true))
                                .build())
                        .build())

                // ── flat options ──
                .option(bool("bossRespawnAlert", defaults.slayer.bossRespawnAlert,
                        () -> config.slayer.bossRespawnAlert, v -> config.slayer.bossRespawnAlert = v))
                .option(bool("noslayerquest", defaults.slayer.noslayerquest,
                        () -> config.slayer.noslayerquest, v -> config.slayer.noslayerquest = v))
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

    private static Option<SlayerBossInfoMode> bossInfo(String key, SlayerBossInfoMode def,
                                                        Supplier<SlayerBossInfoMode> getter,
                                                        Consumer<SlayerBossInfoMode> setter) {
        return Option.<SlayerBossInfoMode>createBuilder()
                .name(Component.translatable("config.babyzombieaddons.option." + key))
                .description(Component.translatable("config.babyzombieaddons.option." + key + ".desc"))
                .binding(def, getter, setter)
                .controller(ConfigUtils.createEnumController(m ->
                        Component.translatable("config.babyzombieaddons.option.slayerBossInfoMode." + m.name())))
                .build();
    }
}
