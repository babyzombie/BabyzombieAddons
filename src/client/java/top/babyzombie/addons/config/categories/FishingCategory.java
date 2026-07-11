package top.babyzombie.addons.config.categories;

import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.azureaaron.dandelion.api.controllers.IntegerController;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.ConfigUtils;
import top.babyzombie.addons.config.ModConfig;

import java.util.function.Supplier;
import java.util.function.Consumer;

public final class FishingCategory {
    private FishingCategory() {}

    public static ConfigCategory create(ModConfig defaults, ModConfig config) {
        return ConfigCategory.createBuilder()
                .id(Identifier.fromNamespaceAndPath("babyzombieaddons", "config/fishing"))
                .name(Component.translatable("config.babyzombieaddons.category.fishing"))

                // ── 稀有海怪 ──
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.rareSeaCreatures"))
                        .option(bool("rareSeaCreaturesAlert", defaults.fishing.rareSeaCreaturesAlert,
                                () -> config.fishing.rareSeaCreaturesAlert,
                                v -> config.fishing.rareSeaCreaturesAlert = v))
                        .option(bool("rareSeaCreaturesAlertTitle", defaults.fishing.rareSeaCreaturesAlertTitle,
                                () -> config.fishing.rareSeaCreaturesAlertTitle,
                                v -> config.fishing.rareSeaCreaturesAlertTitle = v))
                        .build())

                // ── 防瞬间收杆 ──
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("config.babyzombieaddons.group.preventInstantReel"))
                        .option(bool("preventInstantReel", defaults.fishing.preventInstantReel,
                                () -> config.fishing.preventInstantReel,
                                v -> config.fishing.preventInstantReel = v))
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.babyzombieaddons.option.preventInstantReelDelay"))
                                .description(Component.translatable("config.babyzombieaddons.option.preventInstantReelDelay.desc"))
                                .binding(defaults.fishing.preventInstantReelDelay,
                                        () -> config.fishing.preventInstantReelDelay,
                                        v -> config.fishing.preventInstantReelDelay = v)
                                .controller(IntegerController.createBuilder().range(50, 500).slider(25).build())
                                .build())
                        .build())

                // ── 鱼饵不足提醒（指向 popup.popupBaitLow）──
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
}
