package top.babyzombie.addons.module.chat.containerchat;

import com.mojang.blaze3d.platform.InputConstants;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.ContainerClickEvents;

public final class ContainerChatModule {

    private ContainerChatModule() {}

    public static void init() {
        ContainerClickEvents.BEFORE_MOUSE_CLICK.register((screen, slot, event) -> {
            if (!ModConfigManager.get().general.chat.chatInContainer) return false;
            if (ContainerChatHelper.isBlocklistedContainer(screen)) return false;
            if ((event.modifiers() & InputConstants.MOD_ALT) == 0) return false;
            if (slot == null || !slot.hasItem()) return false;

            var stack = slot.getItem();

            // 聊天未打开 → 切换收藏状态（Skyblocker > Firmament > 自维护）
            if (!ContainerChatHelper.isActive()) {
                ItemProtectBridge.toggle(stack);
                return true;
            }

            // 聊天已打开 → 分享物品名，并把焦点交回聊天栏
            if (ContainerChatHelper.getOverlay() != null) {
                ContainerChatHelper.insertShareText(stack);
                return true;
            }
            return false;
        });
    }
}
