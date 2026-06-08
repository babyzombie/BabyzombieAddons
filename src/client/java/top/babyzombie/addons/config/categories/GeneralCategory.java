package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ButtonOption;
import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.controllers.IntegerController;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfig.WorldRenderPhase;
import top.babyzombie.addons.config.hud.HudManager;

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
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.skipSecondPerson"))
                        .description(Component.translatable("config.babyzombieaddons.option.skipSecondPerson.desc"))
                        .binding(defaults.general.skipSecondPerson,
                                () -> config.general.skipSecondPerson,
                                v -> config.general.skipSecondPerson = v)
                        .controller(IntegerController.createBuilder().range(0, 30).slider(1).build())
                        .build())
                .option(createBool("useTpsAdjustedTime", defaults.general.useTpsAdjustedTime,
                        () -> config.general.useTpsAdjustedTime, v -> config.general.useTpsAdjustedTime = v))
                .option(Option.<WorldRenderPhase>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.renderPhase"))
                        .description(Component.translatable("config.babyzombieaddons.option.renderPhase.desc"))
                        .binding(defaults.general.renderPhase,
                                () -> config.general.renderPhase,
                                v -> config.general.renderPhase = v)
                        .controller(ConfigUtils.createEnumController(m ->
                                Component.translatable("config.babyzombieaddons.option.renderPhase." + m.name())))
                        .build())
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
