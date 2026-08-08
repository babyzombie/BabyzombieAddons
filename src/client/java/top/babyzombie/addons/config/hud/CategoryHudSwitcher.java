package top.babyzombie.addons.config.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.util.ChatUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 分类 HUD 切换器（Category Hud Switcher, CHS）
 * 参考 ChestCounter.java 模式实现：renderOnScreen + onScreenClick + setTooltipForNextFrame
 *
 * 交互逻辑：
 * - 长按左键 (>250ms) -> 拖拽移动
 * - 单击左键 (<250ms) -> 打开/关闭标签下拉框
 * - 下拉框中点击某标签 -> HudManager.activeTag 切换，当前屏幕 HUD 立即按该标签筛选
 * - 永远显示，不受 activeTag == MISC 影响
 */
public final class CategoryHudSwitcher {
    private static final String CHS_NAME = "CategoryHudSwitcher";
    private static final long LONG_PRESS_MS = 250L;
    private static final int BOX_PAD = 6;
    private static final int DROPDOWN_W = 140;

    private CategoryHudSwitcher() {}

    private static boolean dropdownOpen;
    private static boolean dragging;
    private static long pressStartMs;
    private static int dragOffX, dragOffY;
    private static boolean pressed;
    private static int lastW;

    public static void init() {
        // 注册已经在 HudRegistrar.register() 中处理
    }

    // ============ ChestCounter 模式：渲染 + Hover ============
    public static void renderOnScreen(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Screen currentScreen = Minecraft.getInstance().screen;
        // 需求 1 / 4：筛选HUD仅在 /bza hud 页面下显示和生效，不影响游戏内其他界面和HUD显示
        if (!(currentScreen instanceof HudEditScreen)) {
            dropdownOpen = false;
            return;
        }
        Font font = Minecraft.getInstance().font;
        int x = HudManager.x(CHS_NAME);
        int y = HudManager.y(CHS_NAME);
        float s = HudManager.scale(CHS_NAME);
        HudTag tag = HudManager.activeTag;
        String title = ChatUtils.translate("config.babyzombieaddons.hud.category.title")
                + tag.toString() + " ▼";
        int tw = font.width(ChatUtils.stripColor(title));
        int boxW = (int) ((tw + BOX_PAD * 2) * s);
        int boxH = (int) ((font.lineHeight + BOX_PAD) * s);
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        // NPE 防御（正常情况下不可能，防御 hud.json 极端异常或注册遗漏时空指针）
        if (HudManager.elements.get(CHS_NAME) == null) {
            HudManager.register(CHS_NAME, sw - 150, 10, 1.0f,
                    ChatUtils.translate("config.babyzombieaddons.hud.chs.fallbackDemo"),
                    "config.babyzombieaddons.option.hudCategorySwitcher",
                    () -> true,
                    HudTag.MISC);
            x = sw - 150; y = 10;
        }

        // 首次进入：若 x == 1600 默认值，实时钳位到 sw - 150
        if (x == 1600 && sw >= 400) {
            x = sw - 150;
            HudManager.elements.get(CHS_NAME).x = x;
        }

        // 强制 clamp：无论 hud.json 保存了什么越界值，都无条件钳位到屏幕内
        x = Math.max(0, Math.min(x, sw - Math.max(boxW, 60)));
        y = Math.max(0, Math.min(y, sh - Math.max(boxH, 10)));
        if (!dragging) {
            HudManager.elements.get(CHS_NAME).x = x;
            HudManager.elements.get(CHS_NAME).y = y;
        }
        lastW = boxW;

        // ===== 渲染 CHS 主体框 =====
        var ps = g.pose();
        ps.pushMatrix();
        ps.translate((float) x, (float) y);
        ps.scale(s, s);
        g.fill(0, 0, tw + BOX_PAD * 2, font.lineHeight + BOX_PAD, 0xCC202020);
        g.fill(0, 0, tw + BOX_PAD * 2, 1, 0xFF555555);
        g.fill(0, font.lineHeight + BOX_PAD - 1, tw + BOX_PAD * 2, font.lineHeight + BOX_PAD, 0xFF555555);
        g.fill(0, 0, 1, font.lineHeight + BOX_PAD, 0xFF555555);
        g.fill(tw + BOX_PAD * 2 - 1, 0, tw + BOX_PAD * 2, font.lineHeight + BOX_PAD, 0xFF555555);
        g.text(font, title, BOX_PAD, BOX_PAD / 2, 0xFFFFFFFF, true);
        ps.popMatrix();

        // ===== hover tooltip =====
        if (!dropdownOpen && !dragging && hitTest(mouseX, mouseY, x, y, boxW, boxH)) {
            String tipKey = "config.babyzombieaddons.hud.category.tooltip";
            g.setTooltipForNextFrame(font,
                    java.util.List.of(Component.translatable(tipKey)),
                    java.util.Optional.empty(), mouseX, mouseY, null);
        }

        // ===== 下拉框渲染 =====
        if (dropdownOpen) {
            HudTag[] tags = HudTag.values();
            List<String> items = new ArrayList<>(tags.length);
            for (HudTag t : tags) items.add(t.toString());
            int sel = Arrays.asList(tags).indexOf(tag);
            int hoverLineH = HudDropdownRenderer.lineHeight(font);
            int hovered = HudDropdownRenderer.hitTestItem(mouseX, mouseY, x, y + boxH, DROPDOWN_W, tags.length, hoverLineH);
            HudDropdownRenderer.drawDropdown(g, x, y + boxH, DROPDOWN_W, items, sel, hovered, font);
        }
    }

    // ============ ChestCounter 模式：点击分发（三回调：press / release / drag） ============
    public static boolean onMouseClicked(MouseButtonEvent event) {
        if (event.button() != 0) return false;
        Screen currentScreen = Minecraft.getInstance().screen;
        if (!(currentScreen instanceof HudEditScreen)) return false;
        Font font = Minecraft.getInstance().font;
        int x = HudManager.x(CHS_NAME);
        int y = HudManager.y(CHS_NAME);
        int boxW = Math.max(lastW, 120);
        int s = (int) HudManager.scale(CHS_NAME);
        int boxH = (int) ((font.lineHeight + BOX_PAD) * Math.max(s, 1));
        int mx = (int) event.x(), my = (int) event.y();

        if (dropdownOpen) {
            int lineH = HudDropdownRenderer.lineHeight(font);
            int ddH = HudTag.values().length * lineH;
            boolean hitDD = hitTest(mx, my, x, y + boxH, DROPDOWN_W, ddH);
            if (hitDD) {
                pressed = true;
                dragging = false;
                pressStartMs = System.currentTimeMillis();
                return true;
            }
            if (!hitTest(mx, my, x, y, boxW, boxH)) {
                dropdownOpen = false;
                return false;
            }
        }

        boolean hitMain = hitTest(mx, my, x, y, boxW, boxH);
        if (!hitMain) return false;
        pressed = true;
        dragging = false;
        pressStartMs = System.currentTimeMillis();
        dragOffX = mx - x;
        dragOffY = my - y;
        return true;
    }

    public static boolean onMouseReleased(MouseButtonEvent event) {
        if (event.button() != 0) return false;
        Screen currentScreen = Minecraft.getInstance().screen;
        if (!(currentScreen instanceof HudEditScreen)) return false;
        if (!pressed) return false;
        Font font = Minecraft.getInstance().font;
        int x = HudManager.x(CHS_NAME);
        int y = HudManager.y(CHS_NAME);
        int boxW = Math.max(lastW, 120);
        int s = (int) HudManager.scale(CHS_NAME);
        int boxH = (int) ((font.lineHeight + BOX_PAD) * Math.max(s, 1));
        int mx = (int) event.x(), my = (int) event.y();

        pressed = false;
        long dur = System.currentTimeMillis() - pressStartMs;

        if (dragging) {
            dragging = false;
            HudManager.save();
            return true;
        }

        if (dropdownOpen) {
            int lineH = HudDropdownRenderer.lineHeight(font);
            int ddH = HudTag.values().length * lineH;
            if (hitTest(mx, my, x, y + boxH, DROPDOWN_W, ddH)) {
                int idx = HudDropdownRenderer.hitTestItem(mx, my, x, y + boxH, DROPDOWN_W, HudTag.values().length, lineH);
                if (idx >= 0 && idx < HudTag.values().length) {
                    HudManager.activeTag = HudTag.values()[idx];
                    HudManager.save();
                }
                dropdownOpen = false;
                return true;
            }
            if (!hitTest(mx, my, x, y, boxW, boxH)) {
                dropdownOpen = false;
                return false;
            }
        }

        if (hitTest(mx, my, x, y, boxW, boxH) && dur < LONG_PRESS_MS) {
            dropdownOpen = !dropdownOpen;
        }
        return true;
    }

    public static boolean onMouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() != 0) return false;
        Screen currentScreen = Minecraft.getInstance().screen;
        if (!(currentScreen instanceof HudEditScreen)) return false;
        if (!pressed) return false;
        Font font = Minecraft.getInstance().font;
        int boxW = Math.max(lastW, 120);
        int s = (int) HudManager.scale(CHS_NAME);
        int boxH = (int) ((font.lineHeight + BOX_PAD) * Math.max(s, 1));
        int mx = (int) event.x(), my = (int) event.y();
        long now = System.currentTimeMillis();
        long dur = now - pressStartMs;

        boolean movedOffChs = !hitTest(mx, my, HudManager.x(CHS_NAME), HudManager.y(CHS_NAME), boxW, boxH);
        if (!dragging && dur < LONG_PRESS_MS && !movedOffChs) return false;

        dragging = true;
        dropdownOpen = false;
        int nx = mx - dragOffX;
        int ny = my - dragOffY;
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        nx = Math.max(0, Math.min(nx, sw - Math.max(boxW, 20)));
        ny = Math.max(0, Math.min(ny, sh - Math.max(boxH, 10)));
        HudManager.elements.get(CHS_NAME).x = nx;
        HudManager.elements.get(CHS_NAME).y = ny;
        return true;
    }

    private static boolean hitTest(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
