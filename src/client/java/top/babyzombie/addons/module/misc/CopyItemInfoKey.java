package top.babyzombie.addons.module.misc;

import net.minecraft.client.KeyMapping;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.KeyBindingUtil;

/**
 * 在容器界面指着物品按下按键时复制物品信息。
 * 实际按键处理在 {@code ContainerClickMixin#onKeyPressed} 中。
 */
public final class CopyItemInfoKey {
    public static KeyMapping KEY;

    private CopyItemInfoKey() {}

    /** 延迟注册（此时 config 已就绪），后续可通过 init() 再次确保已注册 */
    public static void init() {
        if (KEY == null) {
            KEY = KeyBindingUtil.register(
                "key.babyzombieaddons.copy_item_info",
                ModConfigManager.get().misc.copyItemInfo);
        }
    }
}
