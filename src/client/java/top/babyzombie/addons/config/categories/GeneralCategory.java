package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ButtonOption;
import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.azureaaron.dandelion.api.controllers.IntegerController;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfig.AutoISDest;
import top.babyzombie.addons.config.ModConfig.KickRecovery;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.module.raredrop.RareDropScreen;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class GeneralCategory {
    private GeneralCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(Identifier.fromNamespaceAndPath("babyzombieaddons", "config/general"))
                .name(Component.translatable("config.babyzombieaddons.category.general"))
                .option(createBool("updateChecker", defaults.general.updateChecker,
                        () -> config.general.updateChecker, v -> config.general.updateChecker = v))
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.hudEdit"))
                        .description(Component.translatable("config.babyzombieaddons.option.hudEdit.desc"))
                        .prompt(Component.translatable("config.babyzombieaddons.prompt.open"))
                        .action(HudManager::openEditScreen)
                        .build())
                .option(createBool("playCmd", defaults.misc.playCmd,
                        () -> config.misc.playCmd, v -> config.misc.playCmd = v))
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.raredropManage"))
                        .description(Component.translatable("config.babyzombieaddons.option.raredropManage.desc"))
                        .prompt(Component.translatable("config.babyzombieaddons.prompt.open"))
                        .action(screen -> Minecraft.getInstance().setScreen(new RareDropScreen(screen)))
                        .build())
                .option(createBool("abiphoneGui", defaults.misc.abiphoneGui,
                        () -> config.misc.abiphoneGui, v -> config.misc.abiphoneGui = v))
                .option(createBool("autois", defaults.general.autois,
                        () -> config.general.autois, v -> config.general.autois = v))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.autois"))
                        .collapsed(true)
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.autoisDelay"))
                                .description(Component.translatable("config.babyzombieaddons.option.autoisDelay.desc"))
                                .binding(defaults.general.autoisDelay,
                                        () -> config.general.autoisDelay,
                                        v -> config.general.autoisDelay = v)
                                .controller(IntegerController.createBuilder().range(1, 180).slider(1).build())
                                .build())
                        .option(Option.<AutoISDest>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.autoisDest"))
                                .description(Component.translatable("config.babyzombieaddons.option.autoisDest.desc"))
                                .binding(defaults.general.autoisDest,
                                        () -> config.general.autoisDest,
                                        v -> config.general.autoisDest = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.autoisDest." + m.name())))
                                .build())
                        .option(Option.<KickRecovery>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.autoBackToSkyblock"))
                                .description(Component.translatable("config.babyzombieaddons.option.autoBackToSkyblock.desc"))
                                .binding(defaults.general.autoBackToSkyblock,
                                        () -> config.general.autoBackToSkyblock,
                                        v -> config.general.autoBackToSkyblock = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.autoBackToSkyblock." + m.name())))
                                .build())
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.skipSecondPerson"))
                        .description(Component.translatable("config.babyzombieaddons.option.skipSecondPerson.desc"))
                        .binding(defaults.general.skipSecondPerson,
                                () -> config.general.skipSecondPerson,
                                v -> config.general.skipSecondPerson = v)
                        .controller(IntegerController.createBuilder().range(0, 30).slider(1).build())
                        .build())
                .option(createBool("cakeBuffTracker", defaults.general.cakeBuffTracker,
                        () -> config.general.cakeBuffTracker, v -> config.general.cakeBuffTracker = v))
                .build();
    }

    private static Option<Boolean> createBool(String key, boolean def, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("config.babyzombieaddons.option." + key))
                .description(Component.translatable("config.babyzombieaddons.option." + key + ".desc"))
                .binding(def, getter, setter)
                .controller(ConfigUtils.createBooleanController())
                .build();
    }
}
