package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
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
                .option(bool("autoAccept", defaults.party.autoAccept,
                        () -> config.party.autoAccept, v -> config.party.autoAccept = v))
                .option(bool("doublePWarpConfirm", defaults.party.doublePWarpConfirm,
                        () -> config.party.doublePWarpConfirm, v -> config.party.doublePWarpConfirm = v))
                .option(bool("partyCommands", defaults.party.partyCommands,
                        () -> config.party.partyCommands, v -> config.party.partyCommands = v))
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
