package top.babyzombie.addons.module.misc.pet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

import java.util.regex.Pattern;

/**
 * 宠物页面按键处理 —— 在 Pets 容器界面中按配置的按键模拟点击对应槽位。
 * 仅在设置页面配置，不注册到原版按键系统。
 *
 * 槽位布局（6 行 × 9 列容器）：
 *   Row 2-5, Cols 2-8 → 28 个宠物槽位 (slots 10-16, 19-25, 28-34, 37-43)
 *   Row 6, Col 1       → 上一页 (slot 45)
 *   Row 6, Col 9       → 下一页 (slot 53)
 */
public final class PetPageKeyHandler {

    /** 与 PetManager 一致的标题检测模式："Pets" 或 "(1/2) Pets" */
    private static final Pattern PET_MENU_TITLE = Pattern.compile("(\\(\\d+/\\d+\\) )?Pets");

    /** 28 个宠物槽位（行优先，Row 2-5, Cols 2-8） */
    private static final int[] PET_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,  // Row 2, cols 2-8
        19, 20, 21, 22, 23, 24, 25,  // Row 3, cols 2-8
        28, 29, 30, 31, 32, 33, 34,  // Row 4, cols 2-8
        37, 38, 39, 40, 41, 42, 43   // Row 5, cols 2-8
    };

    private static final int PREV_PAGE_SLOT = 45;  // Row 6, Col 1
    private static final int NEXT_PAGE_SLOT = 53;  // Row 6, Col 9

    private PetPageKeyHandler() {}

    /**
     * 处理按键事件。
     *
     * @param keyCode GLFW 键码（来自 KeyEvent.key()）
     * @return true 如果按键被消费（应阻止传递给原版逻辑）
     */
    public static boolean handleKeyPress(int keyCode) {
        var mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) return false;

        // 检测宠物菜单标题
        String title = ChatUtils.stripColor(screen.getTitle().getString());
        if (!PET_MENU_TITLE.matcher(title).matches()) return false;

        var kb = ModConfigManager.get().skyblock.pet.petPageKeyBindings;

        // 翻页键
        if (keyCode == kb.prevPage && kb.prevPage != GLFW.GLFW_KEY_UNKNOWN) {
            clickSlot(screen, PREV_PAGE_SLOT);
            return true;
        }
        if (keyCode == kb.nextPage && kb.nextPage != GLFW.GLFW_KEY_UNKNOWN) {
            clickSlot(screen, NEXT_PAGE_SLOT);
            return true;
        }

        // 宠物槽位键
        int[] slotKeys = kb.slotKeys();
        for (int i = 0; i < slotKeys.length; i++) {
            if (keyCode == slotKeys[i] && slotKeys[i] != GLFW.GLFW_KEY_UNKNOWN) {
                clickSlot(screen, PET_SLOTS[i]);
                return true;
            }
        }

        return false;
    }

    private static void clickSlot(AbstractContainerScreen<?> screen, int slotId) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        mc.gameMode.handleContainerInput(
            screen.getMenu().containerId, slotId, 0,
            ContainerInput.PICKUP, mc.player);
    }
}
