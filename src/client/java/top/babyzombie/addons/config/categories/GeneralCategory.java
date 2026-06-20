package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ButtonOption;
import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.azureaaron.dandelion.api.controllers.FloatController;
import net.azureaaron.dandelion.api.controllers.IntegerController;
import net.azureaaron.dandelion.api.controllers.StringController;
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
                .option(createBool("chatChannelSwitcher", defaults.chatChannel.chatChannelSwitcher,
                        () -> config.chatChannel.chatChannelSwitcher, v -> config.chatChannel.chatChannelSwitcher = v))
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
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.autoReconnect"))
                        .collapsed(true)
                        .option(createBool("autoReconnectEnabled", defaults.general.autoReconnectEnabled,
                                () -> config.general.autoReconnectEnabled, v -> config.general.autoReconnectEnabled = v))
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.autoReconnectDelay"))
                                .description(Component.translatable("config.babyzombieaddons.option.autoReconnectDelay.desc"))
                                .binding(defaults.general.autoReconnectDelay,
                                        () -> config.general.autoReconnectDelay,
                                        v -> config.general.autoReconnectDelay = v)
                                .controller(IntegerController.createBuilder().range(1, 60).slider(1).build())
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.autoReconnectMaxRetries"))
                                .description(Component.translatable("config.babyzombieaddons.option.autoReconnectMaxRetries.desc"))
                                .binding(defaults.general.autoReconnectMaxRetries,
                                        () -> config.general.autoReconnectMaxRetries,
                                        v -> config.general.autoReconnectMaxRetries = v)
                                .controller(IntegerController.createBuilder().range(0, 10).slider(1).build())
                                .build())
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.autoJoinServer"))
                        .collapsed(true)
                        .option(createBool("autoJoinServer", defaults.autoJoin.autoJoinServer,
                                () -> config.autoJoin.autoJoinServer, v -> config.autoJoin.autoJoinServer = v))
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.autoJoinServerIP"))
                                .description(Component.translatable("config.babyzombieaddons.option.autoJoinServerIP.desc"))
                                .binding(defaults.autoJoin.autoJoinServerIP,
                                        () -> config.autoJoin.autoJoinServerIP,
                                        v -> config.autoJoin.autoJoinServerIP = v)
                                .controller(StringController.createBuilder().build())
                                .build())
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.playerScale"))
                        .collapsed(true)
                        .option(Option.<Float>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.playerScaleX"))
                                .description(Component.translatable("config.babyzombieaddons.option.playerScaleX.desc"))
                                .binding(defaults.general.playerScaleX,
                                        () -> config.general.playerScaleX,
                                        v -> config.general.playerScaleX = v)
                                .controller(FloatController.createBuilder().range(0.00f, 1.0f).slider(0.05f).build())
                                .build())
                        .option(Option.<Float>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.playerScaleY"))
                                .description(Component.translatable("config.babyzombieaddons.option.playerScaleY.desc"))
                                .binding(defaults.general.playerScaleY,
                                        () -> config.general.playerScaleY,
                                        v -> config.general.playerScaleY = v)
                                .controller(FloatController.createBuilder().range(0.00f, 1.0f).slider(0.05f).build())
                                .build())
                        .option(Option.<Float>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.playerScaleZ"))
                                .description(Component.translatable("config.babyzombieaddons.option.playerScaleZ.desc"))
                                .binding(defaults.general.playerScaleZ,
                                        () -> config.general.playerScaleZ,
                                        v -> config.general.playerScaleZ = v)
                                .controller(FloatController.createBuilder().range(0.00f, 1.0f).slider(0.05f).build())
                                .build())
                        .option(createBool("showCrosshairInThirdPerson", defaults.general.showCrosshairInThirdPerson,
                                () -> config.general.showCrosshairInThirdPerson, v -> config.general.showCrosshairInThirdPerson = v))
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
