package top.babyzombie.addons.config.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

public final class HudEditScreen extends Screen {
    private final Screen parent;
    private HudManager.HudElement selected;
    private int dragOffsetX, dragOffsetY;

    HudEditScreen(Screen parent) {
        super(Component.translatable("config.babyzombieaddons.option.hudEdit"));
        this.parent = parent;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        gui.fill(0, 0, width, height, 0xC0101010);

        var font = minecraft.font;

        for (var e : HudManager.elements.values()) {
            if (!e.showCondition.getAsBoolean()) continue;
            int w = (int) (e.width * e.scale);
            int h = (int) (e.height * e.scale);
            boolean sel = e == selected;

            gui.fill(e.x, e.y, e.x + w, e.y + h, sel ? 0x5CFFFFFF : 0x3C000000);

            float textScale = e.scale;
            int textW = (int) (font.width(HudManager.getDemoText(e.name)) * textScale);
            int textH = (int) (font.lineHeight * textScale);
            int textX = e.x + (w - textW) / 2;
            int textY = e.y + (h - textH) / 2;
            var ps = gui.pose();
            ps.pushMatrix();
            ps.translate(textX, textY);
            ps.scale(textScale, textScale);
            gui.drawString(font, HudManager.getDemoText(e.name), 0, 0, 0xFFFFFFFF, true);
            ps.popMatrix();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);

        selected = null;
        int mx = (int) event.x(), my = (int) event.y();
        for (var e : HudManager.elements.values()) {
            if (!e.showCondition.getAsBoolean()) continue;
            int w = (int) (e.width * e.scale);
            int h = (int) (e.height * e.scale);
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
        int w = (int) (selected.width * selected.scale);
        int h = (int) (selected.height * selected.scale);
        selected.x = (int) Math.max(0, Math.min(mx - dragOffsetX, sw - w));
        selected.y = (int) Math.max(0, Math.min(my - dragOffsetY, sh - h));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        for (var e : HudManager.elements.values()) {
            if (!e.showCondition.getAsBoolean()) continue;
            int w = (int) (e.width * e.scale);
            int h = (int) (e.height * e.scale);
            if (mx >= e.x && mx <= e.x + w && my >= e.y && my <= e.y + h) {
                e.scale = (float) Math.max(0.3, Math.min(e.scale + scrollY / 10.0, 5.0));
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClose() {
        HudManager.save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }
}
