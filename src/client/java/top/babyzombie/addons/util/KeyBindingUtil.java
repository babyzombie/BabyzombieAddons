package top.babyzombie.addons.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Registers and tracks key bindings under the babyzombieaddons category. */
public final class KeyBindingUtil {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("babyzombieaddons", "main"));
    private static final List<KeyMapping> all = new ArrayList<>();

    private KeyBindingUtil() {}

    /** Register a key binding with the given translation key and default key code. */
    public static KeyMapping register(String translationKey, int defaultKey) {
        var km = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(translationKey, InputConstants.Type.KEYSYM, defaultKey, CATEGORY));
        all.add(km);
        return km;
    }

    /** Return all registered key bindings. */
    public static List<KeyMapping> getAll() { return all; }
}
