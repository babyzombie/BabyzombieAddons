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
        km.setKey(toKey(configKeyCode));
    }

    /**
     * 按 MC 惯例将配置中的裸键码转换为 Key：
     * 0-7 为鼠标按键(GLFW_MOUSE_BUTTON)，其余为键盘键。
     * MoulConfig 的 Keybind 编辑器存的就是这种裸键码，鼠标键必须用 Type.MOUSE 创建，
     * 否则 KeyMapping.matchesMouse 永远匹配不上。
     * 上限用字面量 7(即 GLFW_MOUSE_BUTTON_LAST)，避免直接依赖 GLFW
     * (后续 MC 版本会移除 GLFW)。注意 26.1 的 InputConstants.MOUSE_BUTTON_8 仍是
     * 错误值 0，升到 26.2(已修复为 7)后可改用 InputConstants.MOUSE_BUTTON_8。
     */
    public static InputConstants.Key toKey(int configKeyCode) {
        if (configKeyCode >= 0 && configKeyCode <= 7) {
            return InputConstants.Type.MOUSE.getOrCreate(configKeyCode);
        }
        return InputConstants.Type.KEYSYM.getOrCreate(configKeyCode);
    }

    /**
     * 从 KeyMapping 读取当前键码（GLFW key code）。
     */
    public static int keyCodeFrom(KeyMapping km) {
        return ((KeyMappingAccessor) (Object) km).getBoundKey().getValue();
    }
}
