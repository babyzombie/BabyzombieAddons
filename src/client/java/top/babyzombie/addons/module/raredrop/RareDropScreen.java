package top.babyzombie.addons.module.raredrop;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

public class RareDropScreen extends Screen {
    private final Screen parent;
    private EditBox input;
    private double scroll;
    private int hoverIdx = -1;
    private boolean hoverLeft; // true=blacklist, false=sharelist

    public RareDropScreen(Screen parent) {
        super(Component.translatable("rareropscreen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        input = new EditBox(font, 6, 30, width / 2 - 10, 16, Component.empty());
        input.setHint(Component.translatable("rareropscreen.input.hint"));
        addRenderableWidget(input);
        setInitialFocus(input);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float d) {
        g.fill(0, 0, width, height, 0xC0101010);
        g.drawCenteredString(font, Component.translatable("rareropscreen.title").getString(), width / 2, 8, 0xFFFFFF);

        int sy = (int)scroll;
        g.pose().pushMatrix();
        g.pose().translate(0f, (float)-sy);
        my += sy;
        hoverIdx = -1;

        int PAD = 6;
        int leftW = (width - PAD * 3) / 2;
        int rightX = leftW + PAD * 2;
        int startY = 52;

        // === LEFT: Blacklist ===
        int y = startY;
        g.drawString(font, "§c§l" + Component.translatable("rareropscreen.blacklist").getString(), PAD, y, 0xFFFFFFFF);
        y += 14;

        var bl = new ArrayList<>(RareDropModule.getBlacklist().entrySet());
        for (int i = 0; i < bl.size(); i++) {
            var e = bl.get(i);
            String item = e.getKey();
            boolean enabled = e.getValue();
            if (mx >= PAD && mx <= leftW && my >= y && my < y + 20) { hoverIdx = i; hoverLeft = true; }

            g.drawString(font, "§b" + item, PAD + 4, y + 4, 0xFFFFFFFF);

            // Toggle button
            int tx = leftW - 60;
            g.fill(tx, y, tx + 26, y + 20, enabled ? 0x4000FF00 : 0x20FFFFFF);
            g.drawCenteredString(font, enabled ? "§aON" : "§8--", tx + 13, y + 4, 0xFFFFFFFF);

            // Delete button
            int dx = leftW - 30;
            if (mx >= dx && mx <= leftW && my >= y && my < y + 20) g.fill(dx, y, leftW, y + 20, 0x40FF0000);
            g.drawString(font, "§c✕", dx + 8, y + 4, 0xFFFFFFFF);
            y += 22;
        }
        g.fill(PAD, y, leftW, y + 20, 0x30FFFFFF);
        g.drawString(font, "§a" + Component.translatable("rareropscreen.blacklist.add").getString(), PAD + 4, y + 4, 0xFFFFFFFF);

        // === RIGHT: Share List ===
        y = startY;
        g.drawString(font, "§d§l" + Component.translatable("rareropscreen.sharelist").getString(), rightX, y, 0xFFFFFFFF);
        y += 14;

        var sl = new ArrayList<>(RareDropModule.getShareList().entrySet());
        for (int i = 0; i < sl.size(); i++) {
            var e = sl.get(i);
            String item = e.getKey();
            RareDropModule.ShareMode mode = e.getValue();
            if (mx >= rightX && mx <= width - PAD - 8 && my >= y && my < y + 20) { hoverIdx = i; hoverLeft = false; }

            g.drawString(font, "§b" + item, rightX + 4, y + 4, 0xFFFFFFFF);

            int bx = rightX + leftW - 130;
            // AC
            g.fill(bx, y, bx + 24, y + 20, mode.ac() ? 0x4000FF00 : 0x20FFFFFF);
            g.drawCenteredString(font, mode.ac() ? "§aAC" : "§8AC", bx + 12, y + 4, 0xFFFFFFFF);
            // PC, GC, CC, Del
            bx += 26; g.fill(bx, y, bx + 24, y + 20, mode.pc() ? 0x4000FF00 : 0x20FFFFFF);
            g.drawCenteredString(font, mode.pc() ? "§aPC" : "§8PC", bx + 12, y + 4, 0xFFFFFFFF);
            bx += 26; g.fill(bx, y, bx + 24, y + 20, mode.gc() ? 0x4000FF00 : 0x20FFFFFF);
            g.drawCenteredString(font, mode.gc() ? "§aGC" : "§8GC", bx + 12, y + 4, 0xFFFFFFFF);
            bx += 26; g.fill(bx, y, bx + 24, y + 20, mode.cc() ? 0x4000FF00 : 0x20FFFFFF);
            g.drawCenteredString(font, mode.cc() ? "§aCC" : "§8CC", bx + 12, y + 4, 0xFFFFFFFF);
            bx += 26;
            if (mx >= bx && mx <= bx + 24 && my >= y && my < y + 20)
                g.fill(bx, y, bx + 24, y + 20, 0x40FF0000);
            g.drawCenteredString(font, "§c✕", bx + 12, y + 4, 0xFFFFFFFF);
            y += 22;
        }
        g.fill(rightX, y, width - PAD - 8, y + 20, 0x30FFFFFF);
        g.drawString(font, "§a" + Component.translatable("rareropscreen.sharelist.add").getString(), rightX + 4, y + 4, 0xFFFFFFFF);

        g.pose().popMatrix();

        // Scrollbar
        int items = bl.size() + sl.size();
        int contentH = 300 + items * 22;
        int barH = Math.max(30, height * height / Math.max(contentH, 1));
        int barY = (int)(scroll / Math.max(contentH - height, 1) * (height - barH));
        g.fill(width - 6, 0, width, height, 0x30FFFFFF);
        g.fill(width - 6, barY, width, barY + barH, 0x90FFFFFF);

        // Render input widget on top
        input.render(g, mx, my - sy, d);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (input.mouseClicked(event, doubleClick)) return true;
        double mx = event.x(), my = event.y();
        if (event.buttonInfo().button() != 0) return false;

        int sy = (int)scroll;
        my += sy;

        int PAD = 6;
        int leftW = (width - PAD * 3) / 2;
        int rightX = leftW + PAD * 2;
        int startY = 52;

        // === LEFT: Blacklist ===
        int y = startY + 14;
        var bl = new ArrayList<>(RareDropModule.getBlacklist().entrySet());
        for (int i = 0; i < bl.size(); i++) {
            String item = bl.get(i).getKey();
            // Toggle button
            int tx = leftW - 60;
            if (mx >= tx && mx <= tx + 26 && my >= y && my < y + 20) {
                RareDropModule.toggleBlacklist(item);
                saveAndRefresh(); return true;
            }
            // Delete button
            int dx = leftW - 30;
            if (mx >= dx && mx <= leftW && my >= y && my < y + 20) {
                RareDropModule.removeFromBlacklist(item);
                saveAndRefresh(); return true;
            }
            y += 22;
        }
        if (mx >= PAD && mx <= leftW && my >= y && my < y + 20) {
            String name = input.getValue().trim();
            if (!name.isEmpty()) { RareDropModule.addToBlacklist(name.toLowerCase()); input.setValue(""); saveAndRefresh(); }
            return true;
        }

        // === RIGHT: Share List ===
        y = startY + 14;
        var sl = new ArrayList<>(RareDropModule.getShareList().entrySet());
        for (int i = 0; i < sl.size(); i++) {
            String item = sl.get(i).getKey();
            int bx = rightX + leftW - 130;
            if (mx >= bx && mx <= bx + 24 && my >= y && my < y + 20) { RareDropModule.toggleShareMode(item, 'a'); saveAndRefresh(); return true; }
            bx += 26;
            if (mx >= bx && mx <= bx + 24 && my >= y && my < y + 20) { RareDropModule.toggleShareMode(item, 'p'); saveAndRefresh(); return true; }
            bx += 26;
            if (mx >= bx && mx <= bx + 24 && my >= y && my < y + 20) { RareDropModule.toggleShareMode(item, 'g'); saveAndRefresh(); return true; }
            bx += 26;
            if (mx >= bx && mx <= bx + 24 && my >= y && my < y + 20) { RareDropModule.toggleShareMode(item, 'o'); saveAndRefresh(); return true; }
            bx += 26;
            if (mx >= bx && mx <= bx + 24 && my >= y && my < y + 20) { RareDropModule.removeFromShareList(item); saveAndRefresh(); return true; }
            y += 22;
        }
        // Share add button
        if (mx >= rightX && mx <= width - PAD - 8 && my >= y && my < y + 20) {
            String name = input.getValue().trim();
            if (!name.isEmpty()) { RareDropModule.addToShareList(name.toLowerCase()); input.setValue(""); saveAndRefresh(); }
            return true;
        }
        return false;
    }

    private void saveAndRefresh() { RareDropModule.saveLists(); }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Math.max(0, scroll - sy * 20);
        return true;
    }

    @Override
    public void onClose() {
        RareDropModule.saveLists();
        if (parent != null) Minecraft.getInstance().setScreen(parent);
        else super.onClose();
    }
}
