package top.babyzombie.addons.config.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.config.ModConfigManager;

import java.util.ArrayList;
import java.util.List;

public final class HudEditScreen extends Screen {
    private static final int SNAP_THRESHOLD = 6;

    private final Screen parent;
    private HudManager.HudElement selected;
    private HudManager.HudElement hovered;
    private int dragOffsetX, dragOffsetY;
    private int snapLineX = -1; // -1 = 无吸附指示线
    private int snapLineY = -1;

    HudEditScreen(Screen parent) {
        super(Component.translatable("config.babyzombieaddons.option.hudEdit"));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        gui.fill(0, 0, width, height, 0xC0101010);

        var font = minecraft.font;
        hovered = null;

        for (var e : HudManager.elements.values()) {
            if (!showElement(e)) continue;
            float textScale = e.scale;
            String demoText = HudManager.getDemoText(e.name);
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
            if (!key.isEmpty()) {
                gui.setComponentTooltipForNextFrame(font,
                        java.util.List.of(Component.translatable(key)), mouseX, mouseY);
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
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);

        selected = null;
        int mx = (int) event.x(), my = (int) event.y();
        for (var e : HudManager.elements.values()) {
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
        if (selected == null || event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT)
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

        for (var e : HudManager.elements.values()) {
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

        for (var e : HudManager.elements.values()) {
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
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            selected = null;
            snapLineX = -1;
            snapLineY = -1;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        for (var e : HudManager.elements.values()) {
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

    private boolean showElement(HudManager.HudElement e) {
        return e.showCondition.getAsBoolean() || ModConfigManager.get().misc.debugMode;
    }

    private int demoWidth(HudManager.HudElement e) {
        int maxW = 0;
        for (String line : e.demoText.split("\n", -1))
            maxW = Math.max(maxW, (int) (minecraft.font.width(line) * e.scale));
        return maxW;
    }

    private int demoHeight(HudManager.HudElement e) {
        int lines = e.demoText.split("\n", -1).length;
        return (int) (minecraft.font.lineHeight * lines * e.scale);
    }

    @Override
    public void onClose() {
        HudManager.save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }
}
