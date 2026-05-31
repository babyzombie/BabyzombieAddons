package top.babyzombie.addons.module.raredrop;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.util.ChatUtils;

import java.util.*;

/**
 * GUI for managing rare drop blacklist and share list.
 * Accessed from the settings page.
 */
public class RareDropScreen extends Screen {
    private final Screen parent;
    private EditBox input;
    private String status = "";
    private int statusColor = 0x55FF55;

    public RareDropScreen(Screen parent) {
        super(Component.literal("Rare Drop Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int leftW = (width - 20) / 2;
        int rightX = leftW + 30;
        int y = 30;

        // Input field
        input = new EditBox(font, 10, y, leftW - 10, 16, Component.literal(""));
        input.setHint(Component.literal("Item name..."));
        addRenderableWidget(input);

        // Blacklist section
        addRenderableWidget(Button.builder(Component.literal("§cBlacklist (Ignore)"), b -> {})
                .bounds(10, y + 22, leftW, 14).build());

        var bl = new ArrayList<>(RareDropModule.getBlacklist());
        for (int i = 0; i < bl.size(); i++) {
            final String item = bl.get(i);
            int by = y + 40 + i * 22;
            if (by > height - 60) break;
            addRenderableWidget(Button.builder(Component.literal("§7" + item), b -> {})
                    .bounds(14, by, leftW - 50, 20).build());
            addRenderableWidget(Button.builder(Component.literal("§c✕"), b -> {
                RareDropModule.removeFromBlacklist(item);
                rebuildWidgets();
            }).bounds(leftW - 34, by, 30, 20).build());
        }
        int addY = y + 40 + bl.size() * 22;
        if (addY < height - 60) {
            addRenderableWidget(Button.builder(Component.literal("§a+ Add to Blacklist"), b -> {
                String name = input.getValue().trim();
                if (!name.isEmpty()) { RareDropModule.addToBlacklist(name.toLowerCase()); input.setValue(""); rebuildWidgets(); }
            }).bounds(10, addY, leftW, 20).build());
        }

        // Share list section
        int ry = y;
        addRenderableWidget(Button.builder(Component.literal("§dShare List (Auto-send)"), b -> {})
                .bounds(rightX, ry + 22, leftW, 14).build());

        var sl = new ArrayList<>(RareDropModule.getShareList().entrySet());
        for (int i = 0; i < sl.size(); i++) {
            String item = sl.get(i).getKey();
            RareDropModule.ShareMode mode = sl.get(i).getValue();
            int by = ry + 40 + i * 22;
            if (by > height - 60) break;

            addRenderableWidget(Button.builder(Component.literal("§7" + item), b -> {})
                    .bounds(rightX + 4, by, leftW - 114, 20).build());
            addRenderableWidget(Button.builder(Component.literal(mode.ac() ? "§aAC" : "§8AC"), b -> {
                RareDropModule.toggleShareMode(item, 'a'); rebuildWidgets();
            }).bounds(rightX + leftW - 108, by, 24, 20).build());
            addRenderableWidget(Button.builder(Component.literal(mode.pc() ? "§aPC" : "§8PC"), b -> {
                RareDropModule.toggleShareMode(item, 'p'); rebuildWidgets();
            }).bounds(rightX + leftW - 82, by, 24, 20).build());
            addRenderableWidget(Button.builder(Component.literal(mode.gc() ? "§aGC" : "§8GC"), b -> {
                RareDropModule.toggleShareMode(item, 'g'); rebuildWidgets();
            }).bounds(rightX + leftW - 56, by, 24, 20).build());
            addRenderableWidget(Button.builder(Component.literal(mode.cc() ? "§aCC" : "§8CC"), b -> {
                RareDropModule.toggleShareMode(item, 'o'); rebuildWidgets();
            }).bounds(rightX + leftW - 30, by, 24, 20).build());
        }
        int saddY = ry + 40 + sl.size() * 22;
        if (saddY < height - 60) {
            addRenderableWidget(Button.builder(Component.literal("§a+ Add to Share List"), b -> {
                String name = input.getValue().trim();
                if (!name.isEmpty()) { RareDropModule.addToShareList(name.toLowerCase()); input.setValue(""); rebuildWidgets(); }
            }).bounds(rightX, saddY, leftW, 20).build());
        }

        // Status
        if (!status.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal(status), b -> {}).bounds(10, height - 30, width - 20, 20).build());
        }

        // Close button
        addRenderableWidget(Button.builder(Component.literal("§6Done"), b -> onClose())
                .bounds(width / 2 - 40, height - 55, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float d) {
        renderBackground(g, mx, my, d);
        g.drawCenteredString(font, "Rare Drop Manager", width / 2, 8, 0xFFFFFF);
        super.render(g, mx, my, d);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        RareDropModule.saveLists();
        if (parent != null) Minecraft.getInstance().setScreen(parent);
        else super.onClose();
    }
}
