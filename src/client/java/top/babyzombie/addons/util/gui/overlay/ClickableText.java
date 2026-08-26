package top.babyzombie.addons.util.gui.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.util.ChatUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 可点击+悬浮Tooltip的文本组件（严格 ChestCounter 模式）。
 *
 * screenX/screenY 为 GUI 空间左上角坐标（HudManager.x/y 对应坐标），组件在此基础上相对偏移。
 * scale 只缩放渲染尺寸，不改变 screenX/screenY（与 HudManager 风格一致）。
 */
public final class ClickableText {
    public final int relX;
    public final int relY;
    public final String displayText;
    public final int color;
    public final List<String> tooltipKeys;
    public final Runnable onClickLeft;

    public ClickableText(int relX, int relY, String displayText, int color,
                         List<String> tooltipKeys, Runnable onClickLeft) {
        this.relX = relX;
        this.relY = relY;
        this.displayText = displayText == null ? "" : displayText;
        this.color = color;
        this.tooltipKeys = tooltipKeys == null ? Collections.emptyList() : tooltipKeys;
        this.onClickLeft = onClickLeft;
    }

    public int getWidth(Font f) { return measureWidth(f, displayText); }

    /**
     * Measure legacy-formatted text width, including bold extra pixels.
     * This keeps hit-test bounds aligned with what GuiGraphicsExtractor.text actually renders.
     */
    public static int measureWidth(Font f, String text) {
        if (text == null || text.isEmpty()) return 0;
        int width = 0;
        boolean bold = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(++i));
                // Minecraft color codes reset formatting, including bold.
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f') || code == 'x' || code == 'r') {
                    bold = false;
                } else if (code == 'l') {
                    bold = true;
                }
                continue;
            }
            width += f.width(String.valueOf(ch));
            if (bold && ch != ' ') width += 1;
        }
        return width;
    }

    public int getHeight(Font f) { return f.lineHeight; }

    public void render(GuiGraphicsExtractor g, Font font, int screenX, int screenY,
                       float scale, int mouseX, int mouseY) {
        int baseX = screenX + relX;
        int baseY = screenY + relY;

        if (scale != 1f) {
            var ps = g.pose(); ps.pushMatrix();
            ps.translate(baseX, baseY); ps.scale(scale, scale);
            g.text(font, displayText, 0, 0, color, true);
            ps.popMatrix();
        } else {
            g.text(font, displayText, baseX, baseY, color, true);
        }

        if (hitTest(mouseX, mouseY, screenX, screenY, scale, font) && !tooltipKeys.isEmpty()) {
            List<Component> comps = new ArrayList<>(tooltipKeys.size());
            for (String k : tooltipKeys) {
                if (k.startsWith("§") || k.contains(" ")) comps.add(Component.literal(k));
                else comps.add(Component.translatable(k));
            }
            g.setTooltipForNextFrame(Minecraft.getInstance().font, comps,
                    java.util.Optional.empty(), mouseX, mouseY, null);
        }
    }

    public boolean hitTest(int mx, int my, int screenX, int screenY, float scale, Font font) {
        int w = Math.max(1, Math.round(getWidth(font) * scale));
        int h = Math.max(1, Math.round(getHeight(font) * scale));
        int ax = screenX + relX; int ay = screenY + relY;
        return mx >= ax && mx < ax + w && my >= ay && my < ay + h;
    }

    public void click() {
        if (onClickLeft != null) onClickLeft.run();
    }
}
