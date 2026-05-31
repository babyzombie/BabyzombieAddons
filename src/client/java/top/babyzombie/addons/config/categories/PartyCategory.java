package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.azureaaron.dandelion.api.controllers.IntegerController;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class PartyCategory {
    private PartyCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.babyzombieaddons.category.party"))
                .option(bool("doublePWarpConfirm", defaults.party.doublePWarpConfirm,
                        () -> config.party.doublePWarpConfirm, v -> config.party.doublePWarpConfirm = v))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.partyChatCommands"))
                        .collapsed(true)
                        .option(bool("partyAllinvite", defaults.party.partyAllinvite,
                                () -> config.party.partyAllinvite, v -> config.party.partyAllinvite = v))
                        .option(bool("partyInvite", defaults.party.partyInvite,
                                () -> config.party.partyInvite, v -> config.party.partyInvite = v))
                        .option(bool("partyWarp", defaults.party.partyWarp,
                                () -> config.party.partyWarp, v -> config.party.partyWarp = v))
                        .option(bool("partyWarpDelay", defaults.party.partyWarpDelay,
                                () -> config.party.partyWarpDelay, v -> config.party.partyWarpDelay = v))
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.partyWarpDelaySeconds"))
                                .description(Component.translatable("config.babyzombieaddons.option.partyWarpDelaySeconds.desc"))
                                .binding(defaults.party.partyWarpDelaySeconds,
                                        () -> config.party.partyWarpDelaySeconds,
                                        v -> config.party.partyWarpDelaySeconds = v)
                                .controller(IntegerController.createBuilder().range(1, 30).slider(1).build())
                                .build())
                        .option(bool("partyJoinInstance", defaults.party.partyJoinInstance,
                                () -> config.party.partyJoinInstance, v -> config.party.partyJoinInstance = v))
                        .option(bool("partySendCoords", defaults.party.partySendCoords,
                                () -> config.party.partySendCoords, v -> config.party.partySendCoords = v))
                        .option(bool("dmPartyInvite", defaults.party.dmPartyInvite,
                                () -> config.party.dmPartyInvite, v -> config.party.dmPartyInvite = v))
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
