package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.controllers.IntegerController;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.module.popup.PopupEventsModule.PopupSound;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class PopupCategory {
    private PopupCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(net.minecraft.resources.Identifier.fromNamespaceAndPath("babyzombieaddons", "config/popup"))
                .name(Component.translatable("config.babyzombieaddons.category.popup"))
                .description(Component.translatable("config.babyzombieaddons.category.popup.desc"))
                .option(bool("popupPartyInvite", defaults.popup.popupPartyInvite,
                        () -> config.popup.popupPartyInvite, v -> config.popup.popupPartyInvite = v))
                .option(bool("popupGuildPartyInvite", defaults.popup.popupGuildPartyInvite,
                        () -> config.popup.popupGuildPartyInvite, v -> config.popup.popupGuildPartyInvite = v))
                .option(bool("popupFriendRequest", defaults.popup.popupFriendRequest,
                        () -> config.popup.popupFriendRequest, v -> config.popup.popupFriendRequest = v))
                .option(bool("popupDuelsRequest", defaults.popup.popupDuelsRequest,
                        () -> config.popup.popupDuelsRequest, v -> config.popup.popupDuelsRequest = v))
                .option(bool("popupSkyblockTrade", defaults.popup.popupSkyblockTrade,
                        () -> config.popup.popupSkyblockTrade, v -> config.popup.popupSkyblockTrade = v))
                .option(bool("popupDungeonRestart", defaults.popup.popupDungeonRestart,
                        () -> config.popup.popupDungeonRestart, v -> config.popup.popupDungeonRestart = v))
                .option(sound("popupSound", defaults.popup.popupSound,
                        () -> config.popup.popupSound, v -> config.popup.popupSound = v))
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.option.popupBaitLow"))
                        .description(Component.translatable("config.babyzombieaddons.option.popupBaitLow.desc"))
                        .binding(defaults.popup.popupBaitLow,
                                () -> config.popup.popupBaitLow,
                                v -> config.popup.popupBaitLow = v)
                        .controller(IntegerController.createBuilder().range(0, 64).slider(2).build())
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

    private static Option<PopupSound> sound(String key, PopupSound def,
                                            Supplier<PopupSound> getter, Consumer<PopupSound> setter) {
        return Option.<PopupSound>createBuilder()
                .name(Component.translatable("config.babyzombieaddons.option." + key))
                .description(Component.translatable("config.babyzombieaddons.option." + key + ".desc"))
                .binding(def, getter, setter)
                .controller(ConfigUtils.createEnumDropdownController(
                        v -> Component.translatable("babyzombieaddons.popup.sound." + v.key)))
                .build();
    }
}
