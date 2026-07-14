package top.babyzombie.addons.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.mixin.screen.KeyMappingAccessor;

import java.util.ArrayList;
import java.util.List;

public final class KeyBindingUtil {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("babyzombieaddons", "main"));
    private static final List<KeyMapping> all = new ArrayList<>();

    private KeyBindingUtil() {}

    public static KeyMapping register(String translationKey, int defaultKey) {
        var km = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(translationKey, InputConstants.Type.KEYSYM, defaultKey, CATEGORY));
        all.add(km);
        return km;
    }

    public static List<KeyMapping> getAll() { return all; }

    /**
     * 将配置中的 int 键码同步到 KeyMapping 对象。
     */
    public static void syncToKeyMapping(KeyMapping km, int configKeyCode) {
        km.setKey(InputConstants.Type.KEYSYM.getOrCreate(configKeyCode));
    }

    /**
     * 从 KeyMapping 读取当前键码（GLFW key code）。
     */
    public static int keyCodeFrom(KeyMapping km) {
        return ((KeyMappingAccessor) (Object) km).getBoundKey().getValue();
    }
}
