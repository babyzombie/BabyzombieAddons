package top.babyzombie.addons.module.misc;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.util.KeyBindingUtil;

/**
 * 在容器界面指着物品按下按键时复制物品信息。
 * 实际按键处理在 {@code ContainerClickMixin#onKeyPressed} 中。
 */
public final class CopyItemInfoKey {
    public static final KeyMapping KEY = KeyBindingUtil.register(
            "key.babyzombieaddons.copy_item_info", GLFW.GLFW_KEY_UNKNOWN);

    private CopyItemInfoKey() {}

    /** 仅确保 key 类被加载，key 即被注册 */
    public static void init() {}
}
