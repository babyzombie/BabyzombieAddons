package top.babyzombie.addons.config.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.config.ModConfigManager;

public final class HudEditScreen extends Screen {
    private final Screen parent;
    private HudManager.HudElement selected;
    private HudManager.HudElement hovered;
    private int dragOffsetX, dragOffsetY;

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
        selected.x = (int) Math.max(0, Math.min(mx - dragOffsetX, sw - w));
        selected.y = (int) Math.max(0, Math.min(my - dragOffsetY, sh - h));
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            selected = null;
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
                e.scale = (float) Math.max(0.3, Math.min(e.scale + scrollY / 10.0, 5.0));
                return true;
            }
        }
        return false;
    }

    private boolean showElement(HudManager.HudElement e) {
        return e.showCondition.getAsBoolean() || ModConfigManager.get().debug.debugMode;
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
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }
}
