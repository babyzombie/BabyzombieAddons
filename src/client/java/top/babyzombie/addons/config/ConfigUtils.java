package top.babyzombie.addons.config;

import net.azureaaron.dandelion.api.controllers.BooleanController;
import net.azureaaron.dandelion.api.controllers.BooleanController.BooleanStyle;
import net.azureaaron.dandelion.api.controllers.ColourController;
import net.azureaaron.dandelion.api.controllers.EnumController;
import net.azureaaron.dandelion.api.controllers.StringController;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

public final class ConfigUtils {

    public static BooleanController createBooleanController() {
        return BooleanController.createBuilder()
                .coloured(true)
                .booleanStyle(BooleanStyle.YES_NO)
                .build();
    }

    public static ColourController createColourController(boolean hasAlpha) {
        return ColourController.createBuilder().hasAlpha(hasAlpha).build();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> EnumController<T> createEnumController() {
        return (EnumController<T>) EnumController.createBuilder().build();
    }

    public static <T extends Enum<T>> EnumController<T> createEnumController(Function<T, Component> formatter) {
        return EnumController.<T>createBuilder().formatter(formatter).build();
    }

    public static <T extends Enum<T>> EnumController<T> createEnumDropdownController(Function<T, Component> formatter) {
        return EnumController.<T>createBuilder().formatter(formatter).dropdown(true).build();
    }

    public static StringController createStringController() {
        return StringController.createBuilder().build();
    }
}
