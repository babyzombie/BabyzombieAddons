package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class PopupCategory {
    private PopupCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.babyzombieaddons.category.popup"))
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
                .build();
    }

    private static Option<Boolean> bool(String key, boolean def, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("config.babyzombieaddons.option." + key))
                .binding(def, getter, setter)
                .controller(ConfigUtils.createBooleanController())
                .build();
    }
}
