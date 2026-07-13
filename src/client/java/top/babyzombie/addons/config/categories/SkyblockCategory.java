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
import top.babyzombie.addons.config.ModConfig.BzGetFromSacksMode;
import top.babyzombie.addons.config.ModConfig.EntityRenderMode;
import top.babyzombie.addons.config.ModConfig.KickRecovery;
import top.babyzombie.addons.module.raredrop.RareDropScreen;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class SkyblockCategory {
    private SkyblockCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(Identifier.fromNamespaceAndPath("babyzombieaddons", "config/skyblock"))
                .name(Component.translatable("config.babyzombieaddons.category.skyblock"))
                .option(createBool("abiphoneGui", defaults.misc.abiphoneGui,
                        () -> config.misc.abiphoneGui, v -> config.misc.abiphoneGui = v))
                .option(Option.<BzGetFromSacksMode>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.bzGetFromSacks"))
                        .description(Component.translatable("config.babyzombieaddons.option.bzGetFromSacks.desc"))
                        .binding(defaults.misc.bzGetFromSacks,
                                () -> config.misc.bzGetFromSacks,
                                v -> config.misc.bzGetFromSacks = v)
                        .controller(ConfigUtils.createEnumController(m ->
                                Component.translatable("config.babyzombieaddons.option.bzGetFromSacks." + m.name())))
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.raredropManage"))
                        .description(Component.translatable("config.babyzombieaddons.option.raredropManage.desc"))
                        .prompt(Component.translatable("config.babyzombieaddons.prompt.open"))
                        .action(screen -> Minecraft.getInstance().setScreenAndShow(new RareDropScreen(screen)))
                        .build())
                .option(createBool("cakeBuffTracker", defaults.general.cakeBuffTracker,
                        () -> config.general.cakeBuffTracker, v -> config.general.cakeBuffTracker = v))
                .option(createBool("minionCollectAutoClose", defaults.general.minionCollectAutoClose,
                        () -> config.general.minionCollectAutoClose, v -> config.general.minionCollectAutoClose = v))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.autois"))
                        .collapsed(true)
                        .option(createBool("autois", defaults.general.autois,
                                () -> config.general.autois, v -> config.general.autois = v))
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.autoisDelay"))
                                .description(Component.translatable("config.babyzombieaddons.option.autoisDelay.desc"))
                                .binding(defaults.general.autoisDelay,
                                        () -> config.general.autoisDelay,
                                        v -> config.general.autoisDelay = v)
                                .controller(IntegerController.createBuilder().range(5, 125).slider(5).build())
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
                        .option(createBool("hideEntities", defaults.general.hideEntities,
                                () -> config.general.hideEntities, v -> config.general.hideEntities = v))
                        .option(createBool("backOnServerRestart", defaults.general.backOnServerRestart,
                                () -> config.general.backOnServerRestart, v -> config.general.backOnServerRestart = v))
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
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.babyzombieaddons.group.pet"))
                    .collapsed(true)
                    .option(createBool("petDisplay", defaults.pet.petDisplay,
                            () -> config.pet.petDisplay, v -> config.pet.petDisplay = v))
                    .option(createBool("petExpDisplay", defaults.pet.petExpDisplay,
                            () -> config.pet.petExpDisplay, v -> config.pet.petExpDisplay = v))
                    .option(createBool("petItemDisplay", defaults.pet.petItemDisplay,
                            () -> config.pet.petItemDisplay, v -> config.pet.petItemDisplay = v))
                    .option(createBool("petItemIconDisplay", defaults.pet.petItemIconDisplay,
                            () -> config.pet.petItemIconDisplay, v -> config.pet.petItemIconDisplay = v))
                    .option(createBool("petSharedDisplay", defaults.pet.petSharedDisplay,
                            () -> config.pet.petSharedDisplay, v -> config.pet.petSharedDisplay = v))
                    .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.necronBlade"))
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.necronBladeExplosionVolume"))
                                .description(Component.translatable("config.babyzombieaddons.option.necronBladeExplosionVolume.desc"))
                                .binding((int)(defaults.misc.necronBladeExplosionVolume * 100f),
                                        () -> (int)(config.misc.necronBladeExplosionVolume * 100f),
                                        v -> config.misc.necronBladeExplosionVolume = v / 100f)
                                .controller(IntegerController.createBuilder().range(0, 100).slider(5).build())
                                .build())
                        .option(createBool("necronBladeHideExplosionParticles",
                                defaults.misc.necronBladeHideExplosionParticles,
                                () -> config.misc.necronBladeHideExplosionParticles,
                                v -> config.misc.necronBladeHideExplosionParticles = v))
                        .option(createBool("necronBladeHideOthersParticles",
                                defaults.misc.necronBladeHideOthersParticles,
                                () -> config.misc.necronBladeHideOthersParticles,
                                v -> config.misc.necronBladeHideOthersParticles = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.loadout"))
                        .collapsed(true)
                        .option(createBool("loadoutGui", defaults.loadout.enabled,
                                () -> config.loadout.enabled, v -> config.loadout.enabled = v))
                        .option(Option.<EntityRenderMode>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.loadoutEntityRenderMode"))
                                .description(Component.translatable("config.babyzombieaddons.option.loadoutEntityRenderMode.desc"))
                                .binding(defaults.loadout.entityRenderMode,
                                        () -> config.loadout.entityRenderMode,
                                        v -> config.loadout.entityRenderMode = v)
                                .controller(ConfigUtils.createEnumController(m ->
                                        Component.translatable("config.babyzombieaddons.option.loadoutEntityRenderMode." + m.name())))
                                .build())
                        .option(createBool("loadoutAutoClose", defaults.loadout.autoClose,
                                () -> config.loadout.autoClose, v -> config.loadout.autoClose = v))
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
