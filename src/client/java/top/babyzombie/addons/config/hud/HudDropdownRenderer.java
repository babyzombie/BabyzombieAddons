package top.babyzombie.addons.config.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public final class HudDropdownRenderer {
    private HudDropdownRenderer() {}

    public static int lineHeight(Font font) { return font.lineHeight + 4; }

    public static int drawDropdown(GuiGraphicsExtractor g, int x, int y, int w,
                                    List<String> items, int selectedIdx, int hoverIdx, Font font) {
        int lineH = lineHeight(font);
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
        return items.size() * lineH;
    }

    public static int hitTestItem(int mx, int my, int x, int y, int w, int itemCount, int lineH) {
        if (mx < x || mx > x + w || my < y || my > y + itemCount * lineH) return -1;
        int idx = (my - y) / lineH;
        if (idx < 0 || idx >= itemCount) return -1;
        return idx;
    }
}
