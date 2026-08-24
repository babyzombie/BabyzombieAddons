package top.babyzombie.addons.config.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import com.mojang.blaze3d.platform.InputConstants;

import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class HudEditScreen extends Screen {
    private static final int SNAP_THRESHOLD = 6;
    // 屏幕外元素指示箭头
    private static final int ARROW_LEN = 7;       // 箭头线条半长
    private static final float ARROW_HALF_ANGLE = 0.5f; // chevron 半开角(弧度,约 28.6°)
    private static final int ARROW_HIT = 12;      // 箭头命中框边长(悬停/点击)
    private static final int ARROW_HOP = 12;      // 同侧多箭头沿边缘错开间距
    private static final String CHS_NAME = "CategoryHudSwitcher";

    private static final long CHS_LONG_PRESS_MS = 250L;
    private static final int CHS_BOX_PAD = 6;
    private static final int CHS_DROPDOWN_W = 140;

    private final Screen parent;
    private HudManager.HudElement selected;
    private HudManager.HudElement hovered;
    private int dragOffsetX, dragOffsetY;
    private int snapLineX = -1; // -1 = 无吸附指示线
    private int snapLineY = -1;

    // 分类 HUD 切换器（CHS）状态，原 CategoryHudSwitcher 静态字段迁回实例
    private boolean chsDropdownOpen;
    private boolean chsDragging;
    private long chsPressStartMs;
    private int chsDragOffX, chsDragOffY;
    private boolean chsPressed;
    private int chsLastW;

    HudEditScreen(Screen parent) {
        super(Component.translatable("config.babyzombieaddons.option.hudEdit"));
        this.parent = parent;
    }

    private Stream<HudManager.HudElement> visibleElements() {
        // CHS 由本屏单独渲染/交互，不再作为普通可拖拽元素混入列表
        return HudManager.filteredElements().filter(e -> !CHS_NAME.equals(e.name));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        gui.fill(0, 0, width, height, 0xC0101010);

        var font = minecraft.font;
        hovered = null;

        // 顶部显示当前 activeTag 提示
        String tagHint = ChatUtils.translate("config.babyzombieaddons.hud.edit.tagHint", HudManager.activeTag.toString());
        int tw = font.width(ChatUtils.stripColor(tagHint));
        gui.fill(width/2 - tw/2 - 6, 2, width/2 + tw/2 + 6, font.lineHeight + 6, 0x80000000);
        gui.text(font, tagHint, width/2 - tw/2, 5, 0xFFFFFFFF, true);

        for (var e : (Iterable<HudManager.HudElement>) visibleElements()::iterator) {
            if (!showElement(e)) continue;
            float textScale = e.scale;
            String demoText = e.getDemoText();
            String[] parts = demoText.split("\n", -1);
            int textW = 0;
            for (String line : parts)
                textW = Math.max(textW, (int) (font.width(line) * textScale));
            int textH = (int) (font.lineHeight * parts.length * textScale);
            int pad = 4;
            int w = textW + pad * 2;
            int h = textH + pad * 2;
            boolean sel = e == selected;

            gui.fill(e.x, e.y, e.x + w, e.y + h, sel ? 0x5CFFFFFF : 0x3C000000);

            int textX = e.x + pad;
            int textY = e.y + pad;
            var ps = gui.pose();
            ps.pushMatrix();
            ps.translate(textX, textY);
            ps.scale(textScale, textScale);
            for (int i = 0; i < parts.length; i++) {
                gui.text(font, parts[i], 0, i * font.lineHeight, 0xFFFFFFFF, true);
            }
            ps.popMatrix();

            if (mouseX >= e.x && mouseX <= e.x + w && mouseY >= e.y && mouseY <= e.y + h)
                hovered = e;
        }

        // Tooltip for hovered element
        if (hovered != null && selected == null) {
            String key = HudManager.getLabelKey(hovered.name);
            String tag = hovered.mainTag.toString();
            if (!key.isEmpty()) {
                gui.setComponentTooltipForNextFrame(font,
                        java.util.List.of(Component.translatable(key), Component.translatable("babyzombieaddons.hud.mainTagSource",Component.translatable(tag))), mouseX, mouseY);
            }
        }

        // 吸附指示线（拖动时吸附到参考位置时显示）
        if (selected != null) {
            int lineColor = 0xCC00FFFF; // 青色半透明
            if (snapLineX >= 0) {
                gui.fill(snapLineX, 0, snapLineX + 1, height, lineColor);
            }
            if (snapLineY >= 0) {
                gui.fill(0, snapLineY, width, snapLineY + 1, lineColor);
            }
        }

        // 屏幕外元素指示箭头:悬停显示元素 tooltip,点击拉回
        renderOffscreenArrows(gui, mouseX, mouseY);

        // External HUD source tooltip - skip when hovering over our own element to avoid overlap
        top.babyzombie.addons.util.HudSourceTracker.renderTooltipFromScreen(gui, font, mouseX, mouseY, hovered != null);

        // 分类 HUD 切换器（原 CategoryHudSwitcher.renderOnScreen，就地渲染）
        renderCategorySwitcher(gui, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // CHS 鼠标点击拦截优先（本屏内联，避免与全局 Fabric 注册双重触发）
        if (categorySwitcherMouseClicked(event)) return true;
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);

        int mx = (int) event.x(), my = (int) event.y();

        // 屏幕边缘箭头：点击把完全在屏幕外的元素拉到鼠标位置并衔接拖拽
        // (多个箭头同位置重叠时取最后一个命中的，与悬停 tooltip 显示一致)
        OffscreenArrow hitArrow = null;
        for (OffscreenArrow a : computeOffscreenArrows()) {
            if (arrowHit(a, mx, my)) hitArrow = a;
        }
        if (hitArrow != null) {
            pullToMouse(hitArrow, mx, my);
            return true;
        }

        selected = null;
        for (var e : (Iterable<HudManager.HudElement>) visibleElements()::iterator) {
            if (!showElement(e)) continue;
            int w = demoWidth(e) + 8;
            int h = demoHeight(e) + 8;
            if (mx >= e.x && mx <= e.x + w && my >= e.y && my <= e.y + h) {
                selected = e;
                dragOffsetX = mx - e.x;
                dragOffsetY = my - e.y;
                break;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        // CHS 鼠标拖拽拦截优先
        if (categorySwitcherMouseDragged(event, deltaX, deltaY)) return true;
        if (selected == null || event.button() != InputConstants.MOUSE_BUTTON_LEFT)
            return super.mouseDragged(event, deltaX, deltaY);

        int mx = (int) event.x(), my = (int) event.y();
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        int w = demoWidth(selected) + 8;
        int h = demoHeight(selected) + 8;
        int rawX = mx - dragOffsetX;
        int rawY = my - dragOffsetY;

        // 应用吸附
        int snappedX = applySnapX(rawX, w);
        int snappedY = applySnapY(rawY, h);

        selected.x = (int) Math.max(0, Math.min(snappedX, sw - w));
        selected.y = (int) Math.max(0, Math.min(snappedY, sh - h));
        return true;
    }

    /**
     * 水平方向吸附。将元素的左/右边与屏幕边缘、其他元素边缘对齐。
     */
    private int applySnapX(int proposedX, int w) {
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int bestDist = SNAP_THRESHOLD + 1;
        int bestX = proposedX;
        snapLineX = -1;

        // 收集所有参考 X 坐标
        List<Integer> refs = new ArrayList<>();
        refs.add(0);   // 屏幕左
        refs.add(sw);  // 屏幕右

        for (var e : (Iterable<HudManager.HudElement>) visibleElements()::iterator) {
            if (!showElement(e) || e == selected) continue;
            int ew = demoWidth(e) + 8;
            refs.add(e.x);        // 左
            refs.add(e.x + ew);   // 右
        }

        for (int ref : refs) {
            // 左对齐
            int dist = Math.abs(proposedX - ref);
            if (dist < bestDist) { bestDist = dist; bestX = ref; snapLineX = ref; }
            // 右对齐
            dist = Math.abs(proposedX + w - ref);
            if (dist < bestDist) { bestDist = dist; bestX = ref - w; snapLineX = ref; }
        }

        if (bestDist > SNAP_THRESHOLD) {
            snapLineX = -1;
            return proposedX;
        }
        return bestX;
    }

    /**
     * 垂直方向吸附。将元素的顶/底边与屏幕边缘、其他元素边缘对齐。
     */
    private int applySnapY(int proposedY, int h) {
        int sh = minecraft.getWindow().getGuiScaledHeight();
        int bestDist = SNAP_THRESHOLD + 1;
        int bestY = proposedY;
        snapLineY = -1;

        List<Integer> refs = new ArrayList<>();
        refs.add(0);   // 屏幕顶
        refs.add(sh);  // 屏幕底

        for (var e : (Iterable<HudManager.HudElement>) visibleElements()::iterator) {
            if (!showElement(e) || e == selected) continue;
            int eh = demoHeight(e) + 8;
            refs.add(e.y);        // 顶
            refs.add(e.y + eh);   // 底
        }

        for (int ref : refs) {
            // 顶对齐
            int dist = Math.abs(proposedY - ref);
            if (dist < bestDist) { bestDist = dist; bestY = ref; snapLineY = ref; }
            // 底对齐
            dist = Math.abs(proposedY + h - ref);
            if (dist < bestDist) { bestDist = dist; bestY = ref - h; snapLineY = ref; }
        }

        if (bestDist > SNAP_THRESHOLD) {
            snapLineY = -1;
            return proposedY;
        }
        return bestY;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        // CHS 鼠标释放拦截优先
        if (categorySwitcherMouseReleased(event)) return true;
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            selected = null;
            snapLineX = -1;
            snapLineY = -1;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        for (var e : (Iterable<HudManager.HudElement>) visibleElements()::iterator) {
            if (!showElement(e)) continue;
            int w = demoWidth(e) + 8;
            int h = demoHeight(e) + 8;
            if (mx >= e.x && mx <= e.x + w && my >= e.y && my <= e.y + h) {
                e.scale = (float) Math.clamp(e.scale + scrollY / 10.0, 0.3, 5.0);
                return true;
            }
        }
        return false;
    }

    // ==================== 屏幕外元素指示箭头 ====================

    /**
     * 完全在屏幕外的元素：在屏幕边缘画 chevron 箭头指向其方位。
     * 悬停箭头显示与悬停元素一致的 tooltip；点击（mouseClicked）把元素拉回屏内。
     */
    private void renderOffscreenArrows(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        HudManager.HudElement arrowHovered = null;
        for (OffscreenArrow a : computeOffscreenArrows()) {
            boolean hit = arrowHit(a, mouseX, mouseY);
            drawOffscreenArrow(g, a, hit);
            if (hit) {
                arrowHovered = a.element;
                hovered = a.element; // 压制外部 HUD 源 tooltip 与箭头重叠
            }
        }
        if (arrowHovered != null && selected == null) {
            String key = HudManager.getLabelKey(arrowHovered.name);
            String tag = arrowHovered.mainTag.toString();
            if (!key.isEmpty()) {
                g.setComponentTooltipForNextFrame(minecraft.font,
                        java.util.List.of(Component.translatable(key),
                                Component.translatable("babyzombieaddons.hud.mainTagSource",
                                        Component.translatable(tag))),
                        mouseX, mouseY);
            }
        }
    }

    private record OffscreenArrow(HudManager.HudElement element, int tipX, int tipY, double angle) {}

    /**
     * 计算所有完全在屏幕外元素的指示箭头。尖端 = 屏幕中心到元素中心射线与屏幕边缘的交点(内缩),
     * angle 为线条展开方向(指向屏幕中心的反向,弧度)。
     */
    private List<OffscreenArrow> computeOffscreenArrows() {
        List<OffscreenArrow> result = new ArrayList<>();
        if (selected != null) return result; // 拖动中不显示，避免干扰
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        double cx = sw / 2.0, cy = sh / 2.0;
        for (var e : (Iterable<HudManager.HudElement>) visibleElements()::iterator) {
            if (!showElement(e)) continue;
            int w = demoWidth(e) + 8;
            int h = demoHeight(e) + 8;
            // 与屏幕有交集 = 至少部分可见，可直接点选拖回，不需要箭头
            if (e.x + w > 0 && e.x < sw && e.y + h > 0 && e.y < sh) continue;
            double vx = e.x + w / 2.0 - cx, vy = e.y + h / 2.0 - cy;
            double len = Math.sqrt(vx * vx + vy * vy);
            if (len < 1e-6) continue; // 元素中心与屏幕中心重合(不会发生，防御)
            double ux = vx / len, uy = vy / len;
            // 从屏幕中心沿方向射线与屏幕矩形求交(以半轴归一化)，尖端内缩 3px 保证 chevron 在屏内
            double tH = Math.abs(ux) < 1e-9 ? Double.POSITIVE_INFINITY : (sw / 2.0) / Math.abs(ux);
            double tV = Math.abs(uy) < 1e-9 ? Double.POSITIVE_INFINITY : (sh / 2.0) / Math.abs(uy);
            double t = Math.min(tH, tV) - 3.0;
            int tipX = (int) Math.round(cx + ux * t);
            int tipY = (int) Math.round(cy + uy * t);
            tipX = Math.clamp(tipX, 1, Math.max(1, sw - 2));
            tipY = Math.clamp(tipY, 1, Math.max(1, sh - 2));
            // 线条从尖端向屏内展开(指向屏幕中心的方向)，chevron 开口朝屏内、尖端指向元素
            double angle = Math.atan2(-uy, -ux);
            result.add(new OffscreenArrow(e, tipX, tipY, angle));
        }
        return result;
    }

    /** 两根淡蓝色线条组成 chevron，沿真实方位角指向屏幕外元素。 */
    private static void drawOffscreenArrow(GuiGraphicsExtractor g, OffscreenArrow a, boolean hit) {
        int color = hit ? 0xFFFFFFFF : 0xCC00FFFF;
        int x1 = a.tipX + (int) Math.round(Math.cos(a.angle + ARROW_HALF_ANGLE) * ARROW_LEN);
        int y1 = a.tipY + (int) Math.round(Math.sin(a.angle + ARROW_HALF_ANGLE) * ARROW_LEN);
        int x2 = a.tipX + (int) Math.round(Math.cos(a.angle - ARROW_HALF_ANGLE) * ARROW_LEN);
        int y2 = a.tipY + (int) Math.round(Math.sin(a.angle - ARROW_HALF_ANGLE) * ARROW_LEN);
        line(g, a.tipX, a.tipY, x1, y1, color);
        line(g, a.tipX, a.tipY, x2, y2, color);
    }

    /** Bresenham 直线，逐像素 1x1 fill。 */
    private static void line(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        for (;;) {
            g.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }

    private static boolean arrowHit(OffscreenArrow a, int mx, int my) {
        return Math.abs(mx - a.tipX) <= ARROW_HIT / 2 && Math.abs(my - a.tipY) <= ARROW_HIT / 2;
    }

    /** 点击箭头：把元素拉到鼠标位置并立即衔接拖拽（后续 mouseDragged 正常接管）。 */
    private void pullToMouse(OffscreenArrow a, int mx, int my) {
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        HudManager.HudElement e = a.element;
        int w = demoWidth(e) + 8;
        int h = demoHeight(e) + 8;
        // 元素中心对齐鼠标，钳制保证完全可见
        int nx = Math.clamp(mx - w / 2, 0, Math.max(0, sw - w));
        int ny = Math.clamp(my - h / 2, 0, Math.max(0, sh - h));
        e.x = nx;
        e.y = ny;
        selected = e; // 直接进入拖拽状态
        dragOffsetX = mx - nx; // 以本帧鼠标为锚，后续 mouseDragged 保持抓取点
        dragOffsetY = my - ny;
    }

    private boolean showElement(HudManager.HudElement e) {
        boolean cond = e.showCondition.getAsBoolean() || ModConfigManager.get().misc.debugMode;
        boolean tagMatch = (HudManager.activeTag == HudTag.ALL || e.mainTag == HudManager.activeTag);
        return cond && tagMatch;
    }

    private int demoWidth(HudManager.HudElement e) {
        int maxW = 0;
        for (String line : e.getDemoText().split("\n", -1))
            maxW = Math.max(maxW, (int) (minecraft.font.width(line) * e.scale));
        return maxW;
    }

    private int demoHeight(HudManager.HudElement e) {
        int lines = e.getDemoText().split("\n", -1).length;
        return (int) (minecraft.font.lineHeight * lines * e.scale);
    }

    // ==================== 分类 HUD 切换器（原 CategoryHudSwitcher + HudDropdownRenderer） ====================

    private void renderCategorySwitcher(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Font font = minecraft.font;
        int x = HudManager.x(CHS_NAME);
        int y = HudManager.y(CHS_NAME);
        float s = HudManager.scale(CHS_NAME);
        HudTag tag = HudManager.activeTag;
        String title = ChatUtils.translate("config.babyzombieaddons.hud.category.title")
                + tag.toString() + " ▼";
        int tw = font.width(ChatUtils.stripColor(title));
        int boxW = (int) ((tw + CHS_BOX_PAD * 2) * s);
        int boxH = (int) ((font.lineHeight + CHS_BOX_PAD) * s);
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();

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
        x = Math.clamp(x, 0, sw - Math.max(boxW, 60));
        y = Math.clamp(y, 0, sh - Math.max(boxH, 10));
        if (!chsDragging) {
            HudManager.elements.get(CHS_NAME).x = x;
            HudManager.elements.get(CHS_NAME).y = y;
        }
        chsLastW = boxW;

        // ===== 渲染 CHS 主体框 =====
        var ps = g.pose();
        ps.pushMatrix();
        ps.translate((float) x, (float) y);
        ps.scale(s, s);
        g.fill(0, 0, tw + CHS_BOX_PAD * 2, font.lineHeight + CHS_BOX_PAD, 0xCC202020);
        g.fill(0, 0, tw + CHS_BOX_PAD * 2, 1, 0xFF555555);
        g.fill(0, font.lineHeight + CHS_BOX_PAD - 1, tw + CHS_BOX_PAD * 2, font.lineHeight + CHS_BOX_PAD, 0xFF555555);
        g.fill(0, 0, 1, font.lineHeight + CHS_BOX_PAD, 0xFF555555);
        g.fill(tw + CHS_BOX_PAD * 2 - 1, 0, tw + CHS_BOX_PAD * 2, font.lineHeight + CHS_BOX_PAD, 0xFF555555);
        g.text(font, title, CHS_BOX_PAD, CHS_BOX_PAD / 2, 0xFFFFFFFF, true);
        ps.popMatrix();

        // ===== hover tooltip =====
        if (!chsDropdownOpen && !chsDragging && chsHit(mouseX, mouseY, x, y, boxW, boxH)) {
            String tipKey = "config.babyzombieaddons.hud.category.tooltip";
            g.setTooltipForNextFrame(font,
                    List.of(Component.translatable(tipKey)),
                    java.util.Optional.empty(), mouseX, mouseY, null);
        }

        // ===== 下拉框渲染 =====
        if (chsDropdownOpen) {
            HudTag[] tags = HudTag.values();
            List<String> items = new ArrayList<>(tags.length);
            for (HudTag t : tags) items.add(t.toString());
            int sel = Arrays.asList(tags).indexOf(tag);
            int hoverLineH = dropdownLineHeight(font);
            int hovered = dropdownHitTestItem(mouseX, mouseY, x, y + boxH, CHS_DROPDOWN_W, tags.length, hoverLineH);
            drawDropdown(g, x, y + boxH, CHS_DROPDOWN_W, items, sel, hovered, font);
        }
    }

    private boolean categorySwitcherMouseClicked(MouseButtonEvent event) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        Font font = minecraft.font;
        int x = HudManager.x(CHS_NAME);
        int y = HudManager.y(CHS_NAME);
        int boxW = Math.max(chsLastW, 120);
        int s = (int) HudManager.scale(CHS_NAME);
        int boxH = (int) ((font.lineHeight + CHS_BOX_PAD) * Math.max(s, 1));
        int mx = (int) event.x(), my = (int) event.y();

        if (chsDropdownOpen) {
            int lineH = dropdownLineHeight(font);
            int ddH = HudTag.values().length * lineH;
            boolean hitDD = chsHit(mx, my, x, y + boxH, CHS_DROPDOWN_W, ddH);
            if (hitDD) {
                chsPressed = true;
                chsDragging = false;
                chsPressStartMs = System.currentTimeMillis();
                return true;
            }
            if (!chsHit(mx, my, x, y, boxW, boxH)) {
                chsDropdownOpen = false;
                return false;
            }
        }

        boolean hitMain = chsHit(mx, my, x, y, boxW, boxH);
        if (!hitMain) return false;
        chsPressed = true;
        chsDragging = false;
        chsPressStartMs = System.currentTimeMillis();
        chsDragOffX = mx - x;
        chsDragOffY = my - y;
        return true;
    }

    private boolean categorySwitcherMouseReleased(MouseButtonEvent event) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        if (!chsPressed) return false;
        Font font = minecraft.font;
        int x = HudManager.x(CHS_NAME);
        int y = HudManager.y(CHS_NAME);
        int boxW = Math.max(chsLastW, 120);
        int s = (int) HudManager.scale(CHS_NAME);
        int boxH = (int) ((font.lineHeight + CHS_BOX_PAD) * Math.max(s, 1));
        int mx = (int) event.x(), my = (int) event.y();

        chsPressed = false;
        long dur = System.currentTimeMillis() - chsPressStartMs;

        if (chsDragging) {
            chsDragging = false;
            HudManager.save();
            return true;
        }

        if (chsDropdownOpen) {
            int lineH = dropdownLineHeight(font);
            int ddH = HudTag.values().length * lineH;
            if (chsHit(mx, my, x, y + boxH, CHS_DROPDOWN_W, ddH)) {
                int idx = dropdownHitTestItem(mx, my, x, y + boxH, CHS_DROPDOWN_W, HudTag.values().length, lineH);
                if (idx >= 0 && idx < HudTag.values().length) {
                    HudManager.activeTag = HudTag.values()[idx];
                    HudManager.save();
                }
                chsDropdownOpen = false;
                return true;
            }
            if (!chsHit(mx, my, x, y, boxW, boxH)) {
                chsDropdownOpen = false;
                return false;
            }
        }

        if (chsHit(mx, my, x, y, boxW, boxH) && dur < CHS_LONG_PRESS_MS) {
            chsDropdownOpen = !chsDropdownOpen;
        }
        return true;
    }

    private boolean categorySwitcherMouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        if (!chsPressed) return false;
        Font font = minecraft.font;
        int boxW = Math.max(chsLastW, 120);
        int s = (int) HudManager.scale(CHS_NAME);
        int boxH = (int) ((font.lineHeight + CHS_BOX_PAD) * Math.max(s, 1));
        int mx = (int) event.x(), my = (int) event.y();
        long now = System.currentTimeMillis();
        long dur = now - chsPressStartMs;

        boolean movedOffChs = !chsHit(mx, my, HudManager.x(CHS_NAME), HudManager.y(CHS_NAME), boxW, boxH);
        if (!chsDragging && dur < CHS_LONG_PRESS_MS && !movedOffChs) return false;

        chsDragging = true;
        chsDropdownOpen = false;
        int nx = mx - chsDragOffX;
        int ny = my - chsDragOffY;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        nx = Math.clamp(nx, 0, sw - Math.max(boxW, 20));
        ny = Math.clamp(ny, 0, sh - Math.max(boxH, 10));
        HudManager.elements.get(CHS_NAME).x = nx;
        HudManager.elements.get(CHS_NAME).y = ny;
        return true;
    }

    private boolean chsHit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static int dropdownLineHeight(Font font) { return font.lineHeight + 4; }

    private static void drawDropdown(GuiGraphicsExtractor g, int x, int y, int w,
                                     List<String> items, int selectedIdx, int hoverIdx, Font font) {
        int lineH = dropdownLineHeight(font);
        for (int i = 0; i < items.size(); i++) {
            int bg;
            if (i == selectedIdx) bg = 0xFF2A6A2A;
            else if (i == hoverIdx) bg = 0xFF404040;
            else bg = 0xFF202020;
            g.fill(x, y + i * lineH, x + w, y + (i + 1) * lineH, bg);
            if (i > 0) g.fill(x, y + i * lineH, x + w, y + i * lineH + 1, 0xFF555555);
            g.fill(x, y + i * lineH, x + 1, y + (i + 1) * lineH, 0xFF555555);
            g.fill(x + w - 1, y + i * lineH, x + w, y + (i + 1) * lineH, 0xFF555555);
            int color = (i == selectedIdx) ? 0xFF55FF55 : 0xFFFFFFFF;
            g.text(font, items.get(i), x + 4, y + i * lineH + 2, color, true);
        }
        if (!items.isEmpty()) {
            g.fill(x, y, x + w, y + 1, 0xFF555555);
            g.fill(x, y + items.size() * lineH - 1, x + w, y + items.size() * lineH, 0xFF555555);
        }
    }

    private static int dropdownHitTestItem(int mx, int my, int x, int y, int w, int itemCount, int lineH) {
        if (mx < x || mx > x + w || my < y || my > y + itemCount * lineH) return -1;
        int idx = (my - y) / lineH;
        if (idx < 0 || idx >= itemCount) return -1;
        return idx;
    }

    @Override
    public void onClose() {
        HudManager.save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }
}
