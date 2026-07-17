package top.babyzombie.addons.module.misc.abiphone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class AbiphoneContactScreen extends Screen {

    private static final Gson GSON = new GsonBuilder().create();
    private static final Path SETTINGS_FILE = FabricLoader.getInstance().getConfigDir()
        .resolve("babyzombieaddons").resolve("abiphone").resolve("ui_settings.json");

    // persisted
    private int textColor = 0xFFFFFFFF;
    private boolean colorBarVisible = true;
    private final Set<String> favorites = new HashSet<>();
    private final Set<String> autoAnswer = new HashSet<>();

    // search
    private String searchText = "";
    private boolean searchFocused;
    private int searchCursorTicks;
    private boolean shiftDown;
    private boolean searchFilterMode = true;

    private final List<AbiphoneTracker.ItemEntry> contacts;
    private final String uuid;
    private final String profileId;

    private double scrollOffset;
    private int hoveredIndex = -1;

    private int dragIndex = -1;
    private int dragTargetIndex = -1;
    private boolean dragStarted;
    private double dragMouseX, dragMouseY;
    private double dragPickOffsetX, dragPickOffsetY;
    private double dragStartX, dragStartY;

    private Holder<Enchantment> dummyEnchantment;

    // cached layout
    private int panelWidth, gridLeft, gridRight, gridTop, gridBottom;
    private int cols, slotSize, rowHeight;
    private int cachedGridStartX, cachedEffectiveSlot, cachedColGap;
    private boolean layoutDirty = true;

    //contact
    private final Map<String,String> contactsHasDiffNameInCMD = Map.of(
            "Maddox the Slayer", "slayer",
            "Jotraeline Greatforge", "jotraeline",
            "St. Jerry" ,"stjerry",
            "Fear Mongerer", "fearmongerer",
            "Queen Nyx", "nyx",
            "Tia the Fairy", "tiathefairy",
            "Plumber Joe", "plumberjoe"
    );

    public AbiphoneContactScreen(List<AbiphoneTracker.ItemEntry> contacts) {
        super(Component.literal("Abiphone Contacts"));
        this.contacts = new ArrayList<>(contacts);
        var tracker = HypixelLocationTracker.getInstance();
        this.uuid = tracker.getUuid();
        this.profileId = tracker.getProfileId();
        loadSettings();
    }

    private void loadSettings() {
        try {
            if (Files.exists(SETTINGS_FILE)) {
                var json = GSON.fromJson(Files.readString(SETTINGS_FILE), UiSettings.class);
                if (json != null) {
                    if (json.textColor != 0) textColor = json.textColor;
                    colorBarVisible = json.colorBarVisible;
                    if (json.favorites != null) favorites.addAll(json.favorites);
                    if (json.autoAnswer != null) autoAnswer.addAll(json.autoAnswer);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void saveSettings() {
        try {
            Files.createDirectories(SETTINGS_FILE.getParent());
            Files.writeString(SETTINGS_FILE, GSON.toJson(new UiSettings(textColor, colorBarVisible,
                new ArrayList<>(favorites), new ArrayList<>(autoAnswer))));
        } catch (IOException ignored) {
        }
    }

    public static Set<String> getAutoAnswerNames() {
        Set<String> result = new HashSet<>();
        try {
            if (Files.exists(SETTINGS_FILE)) {
                var json = GSON.fromJson(Files.readString(SETTINGS_FILE), UiSettings.class);
                if (json != null && json.autoAnswer != null) result.addAll(json.autoAnswer);
            }
        } catch (IOException ignored) {
        }
        return result;
    }

    private static class UiSettings {
        int textColor;
        boolean colorBarVisible = true;
        List<String> favorites = new ArrayList<>();
        List<String> autoAnswer = new ArrayList<>();
        UiSettings(int c, boolean v, List<String> f, List<String> a) {
            this.textColor = c; this.colorBarVisible = v; this.favorites = f; this.autoAnswer = a;
        }
    }

    @Override
    protected void init() {
        var client = Minecraft.getInstance();
        if (client.level != null) {
            var registry = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var enchant = registry.getValueOrThrow(Enchantments.UNBREAKING);
            this.dummyEnchantment = registry.wrapAsHolder(enchant);
        }
        layoutDirty = true;
    }

    private void recalcLayout() {
        panelWidth = Math.max(70, width / 6);
        int gridAreaLeft = panelWidth + 4;
        int gridAreaWidth = width - gridAreaLeft;
        int margin = gridAreaWidth / 8;
        gridLeft = gridAreaLeft + margin;
        gridRight = width - margin;
        gridTop = 30;
        gridBottom = height - 10;
        slotSize = 42;
        rowHeight = 68;
        int availWidth = gridRight - gridLeft;
        cols = Math.max(3, availWidth / slotSize);
        if (cols > contacts.size() && contacts.size() > 0) cols = contacts.size();
        layoutDirty = false;
    }

    private static int hueToRgb(float hue) {
        float r, g, b;
        float h = (hue % 360f) / 60f;
        int i = (int) h;
        float f = h - i;
        float q = 1f - f;
        switch (i) {
            case 0:  r = 1; g = f; b = 0; break;
            case 1:  r = q; g = 1; b = 0; break;
            case 2:  r = 0; g = 1; b = f; break;
            case 3:  r = 0; g = q; b = 1; break;
            case 4:  r = f; g = 0; b = 1; break;
            default: r = 1; g = 0; b = q; break;
        }
        int ri = (int)(r * 255);
        int gi = (int)(g * 255);
        int bi = (int)(b * 255);
        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    // ---- render ----

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        try {
            extractBackground(gui, mouseX, mouseY, delta);
        } catch (IllegalStateException e) {
            gui.fill(0, 0, width, height, 0xC0101010);
        }
        if (layoutDirty) recalcLayout();
        searchCursorTicks++;

        int availWidth = gridRight - gridLeft;
        int colGap = 10;
        int actualCols = Math.max(3, (availWidth + colGap) / (slotSize + colGap));
        if (actualCols > contacts.size() && contacts.size() > 0) actualCols = Math.max(3, contacts.size());
        int effectiveSlot = (availWidth - colGap * (actualCols - 1)) / actualCols;
        int gridContentWidth = actualCols * effectiveSlot + colGap * (actualCols - 1);
        int gridStartX = gridLeft + (availWidth - gridContentWidth) / 2;
        cols = actualCols;

        cachedGridStartX = gridStartX;
        cachedEffectiveSlot = effectiveSlot;
        cachedColGap = colGap;
        int visibleHeight = gridBottom - gridTop;

        hoveredIndex = -1;
        if (dragIndex < 0) dragTargetIndex = -1;

        List<Integer> visibleIndices = new ArrayList<>();
        String searchLower = searchText.toLowerCase(Locale.ROOT);

        for (int i = 0; i < contacts.size(); i++) {
            AbiphoneTracker.ItemEntry entry = contacts.get(i);
            String name = stripColor(entry.name());
            if (searchFilterMode && !searchLower.isEmpty()
                && !name.toLowerCase(Locale.ROOT).contains(searchLower)) {
                continue;
            }
            visibleIndices.add(i);
        }

        int totalRows = (visibleIndices.size() + actualCols - 1) / actualCols;
        int totalHeight = totalRows * rowHeight;
        int maxScroll = Math.max(0, totalHeight - visibleHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        int scroll = (int) scrollOffset;

        for (int vi = 0; vi < visibleIndices.size(); vi++) {
            int i = visibleIndices.get(vi);
            int row = vi / actualCols;
            int col = vi % actualCols;
            int itemX = gridStartX + col * (effectiveSlot + colGap);
            int itemY = gridTop + row * rowHeight - scroll;

            if (itemY + rowHeight < gridTop || itemY > gridBottom) continue;
            if (dragStarted && dragIndex == i) continue;

            if (mouseX >= itemX && mouseX < itemX + effectiveSlot
                && mouseY >= itemY && mouseY < itemY + rowHeight) {
                hoveredIndex = i;
                if (dragStarted) dragTargetIndex = i;
            }

            AbiphoneTracker.ItemEntry entry = contacts.get(i);
            ItemStack stack = createItemStack(entry);
            boolean isHovered = (i == hoveredIndex);
            boolean isDropTarget = (dragIndex >= 0 && dragTargetIndex == i);
            String entryName = stripColor(entry.name());
            boolean isFavorite = favorites.contains(entryName);
            boolean isSearchMatch = !searchFilterMode && !searchLower.isEmpty()
                && entryName.toLowerCase(Locale.ROOT).contains(searchLower);

            if (isFavorite) {
                gui.fill(itemX - 1, itemY - 1, itemX + effectiveSlot + 1, itemY + rowHeight + 1, 0x60FF69B4);
            } else if (isSearchMatch) {
                gui.fill(itemX - 1, itemY - 1, itemX + effectiveSlot + 1, itemY + rowHeight + 1, 0x60222222);
            }

            if (isDropTarget && dragStarted && dragIndex != i) {
                gui.fill(itemX - 1, itemY - 1, itemX + effectiveSlot + 1, itemY + rowHeight + 1, 0x40FFFF00);
            }

            if (isHovered) {
                gui.fill(itemX - 1, itemY - 1, itemX + effectiveSlot + 1, itemY + rowHeight + 1, 0x40FFFFFF);
                stack = stack.copy();
                if (dummyEnchantment != null) stack.enchant(dummyEnchantment, 1);
            }

            float iconScale = (effectiveSlot - 6) / 16f;
            int iconX = itemX + (effectiveSlot - (int)(16 * iconScale)) / 2;
            int iconY = itemY + 2;
            var pose = gui.pose();
            pose.pushMatrix();
            pose.scale(iconScale, iconScale);
            gui.item(stack, (int)(iconX / iconScale), (int)(iconY / iconScale));
            gui.itemDecorations(font, stack, (int)(iconX / iconScale), (int)(iconY / iconScale));
            pose.popMatrix();

            String name = entryName;
            if (name.isEmpty()) name = entry.name().replaceAll("§.", "");
            float maxWidth = effectiveSlot + 10;
            int nameX = itemX + effectiveSlot / 2;
            int textY = itemY + 2 + (int)(16 * iconScale) + 2;

            if (font.width(name) > maxWidth) {
                String line1 = font.plainSubstrByWidth(name, (int)maxWidth);
                String remainder = name.substring(line1.length());
                String line2 = font.plainSubstrByWidth(remainder, (int)maxWidth);
                if (!line2.equals(remainder)) line2 = line2 + "..";
                gui.centeredText(font, line1, nameX, textY, textColor);
                gui.centeredText(font, line2, nameX, textY + font.lineHeight, textColor);
            } else {
                gui.centeredText(font, name, nameX, textY, textColor);
            }

            if (autoAnswer.contains(entryName)) {
                String phone = "✆";
                int px = itemX + 3;
                int pyIconBottom = itemY + 2 + (int)(16 * iconScale);
                gui.text(font, phone, px, pyIconBottom - font.lineHeight, 0xFF55AAFF, false);
            }
        }

        if (dragStarted && dragIndex >= 0 && dragIndex < contacts.size()) {
            AbiphoneTracker.ItemEntry entry = contacts.get(dragIndex);
            ItemStack dragStack = createItemStack(entry);
            if (dummyEnchantment != null) dragStack.enchant(dummyEnchantment, 1);
            int sx = (int)(dragMouseX - dragPickOffsetX * 1.5);
            int sy = (int)(dragMouseY - dragPickOffsetY * 1.5);
            var pose = gui.pose();
            pose.pushMatrix();
            pose.scale(1.5f, 1.5f);
            gui.item(dragStack, (int)(sx / 1.5f), (int)(sy / 1.5f));
            gui.itemDecorations(font, dragStack, (int)(sx / 1.5f), (int)(sy / 1.5f));
            pose.popMatrix();
        }

        if (maxScroll > 0) {
            int barX = gridRight + 2, barW = 6;
            int barH = Math.max(20, (int)((float)visibleHeight / totalHeight * visibleHeight));
            int barY = gridTop + (int)(scrollOffset / maxScroll * (visibleHeight - barH));
            gui.fill(barX, gridTop, barX + barW, gridBottom, 0x40FFFFFF);
            gui.fill(barX, barY, barX + barW, barY + barH, 0xFFCCCCCC);
        }

        if (dragIndex < 0 && hoveredIndex >= 0 && hoveredIndex < contacts.size()) {
            gui.setTooltipForNextFrame(font, createItemStack(contacts.get(hoveredIndex)), mouseX, mouseY);
        }

        renderPanel(gui, mouseX, mouseY);

        gui.centeredText(font, "Abiphone Contacts (" + contacts.size() + ")", width / 2, 8, 0xFFFFFF);
    }

    // ---- left panel ----

    private void renderPanel(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
        int px = 4, py = 70;
        int pw = panelWidth - 2;

        // color toggle
        int toggleX = px + 4, toggleY = py, toggleW = 10, toggleH = 10;
        gui.text(font, Component.translatable("babyzombieaddons.panel.color"), px + 4 + toggleW + 4, py, 0xFFAAAAAA, false);
        gui.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, 0xFF555555);
        if (colorBarVisible) {
            gui.fill(toggleX + 2, toggleY + 2, toggleX + toggleW - 2, toggleY + toggleH - 2, 0xFF00CC00);
        }
        py += 14;

        // hue bar (only when visible)
        if (colorBarVisible) {
            int barX = px + 4, barY = py, barW = pw - 10, barH = 14;
            gui.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0x30FFFFFF);
            for (int i = 0; i < barW; i++) {
                float hue = (float)i / barW * 360f;
                gui.fill(barX + i, barY, barX + i + 1, barY + barH, hueToRgb(hue));
            }
            py += barH + 6;
        } else {
            py += 4;
        }

        // search mode toggle
        int sToggleX = px + 4, sToggleY = py;
        gui.fill(sToggleX, sToggleY, sToggleX + toggleW, sToggleY + toggleH, 0xFF555555);
        if (searchFilterMode) {
            gui.fill(sToggleX + 2, sToggleY + 2, sToggleX + toggleW - 2, sToggleY + toggleH - 2, 0xFF00CC00);
        }
        gui.text(font, Component.translatable("babyzombieaddons.panel.search"), px + 4 + toggleW + 4, py, 0xFFAAAAAA, false);
        py += 14;
        int sbX = px + 4, sbY = py, sbW = pw - 10, sbH = 14;
        int sbColor = searchFocused ? 0xFFAAAAAA : 0xFF666666;
        gui.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0x80000000);
        gui.fill(sbX, sbY, sbX + sbW, sbY + 1, sbColor);
        gui.fill(sbX, sbY + sbH - 1, sbX + sbW, sbY + sbH, sbColor);
        gui.fill(sbX, sbY, sbX + 1, sbY + sbH, sbColor);
        gui.fill(sbX + sbW - 1, sbY, sbX + sbW, sbY + sbH, sbColor);
        String displayText = searchText;
        int cursorX = sbX + 4;
        if (!displayText.isEmpty()) {
            String trimmed = font.plainSubstrByWidth(displayText, sbW - 8);
            if (!trimmed.equals(displayText)) {
                while (!trimmed.isEmpty() && font.width(trimmed) > sbW - 8)
                    trimmed = trimmed.substring(1);
            }
            gui.text(font, trimmed, sbX + 4, sbY + 3, 0xFFFFFFFF, false);
            cursorX = sbX + 4 + font.width(trimmed);
        }
        if (searchFocused && (searchCursorTicks / 20) % 2 == 0) {
            gui.fill(cursorX, sbY + 2, cursorX + 1, sbY + sbH - 2, 0xFFFFFFFF);
        }
    }

    private boolean panelClick(double mouseX, double mouseY) {
        int px = 4, py = 70;
        int pw = panelWidth - 2;

        int toggleX = px + 4, toggleY = py, toggleW = 10, toggleH = 10;
        if (mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= toggleY && mouseY <= toggleY + toggleH) {
            colorBarVisible = !colorBarVisible;
            saveSettings();
            return true;
        }

        if (colorBarVisible) {
            int barX = px + 4, barY = py + 14, barW = pw - 10, barH = 14;
            if (mouseX >= barX && mouseX <= barX + barW && mouseY >= barY && mouseY <= barY + barH) {
                float hue = (float)(mouseX - barX) / barW * 360f;
                textColor = hueToRgb(hue);
                saveSettings();
                return true;
            }
        }

        int sToggleBaseY = py + 14 + (colorBarVisible ? 14 + 6 : 4);
        int sToggleX = px + 4;
        if (mouseX >= sToggleX && mouseX <= sToggleX + toggleW
            && mouseY >= sToggleBaseY && mouseY <= sToggleBaseY + toggleH) {
            searchFilterMode = !searchFilterMode;
            return true;
        }

        int searchBaseY = sToggleBaseY + 14;
        int sbX = px + 4, sbW = pw - 10, sbH = 14;
        if (mouseX >= sbX && mouseX <= sbX + sbW && mouseY >= searchBaseY && mouseY <= searchBaseY + sbH) {
            searchFocused = true;
            return true;
        } else {
            searchFocused = false;
        }

        return false;
    }

    // ---- input ----

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= scrollY * 20;
        scrollOffset = Math.max(0, scrollOffset);
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int btn = event.buttonInfo().button();

        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && mx < panelWidth) {
            return panelClick(mx, my);
        }

        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && mx >= panelWidth) {
            searchFocused = false;
        }

        if (btn == GLFW.GLFW_MOUSE_BUTTON_RIGHT && hoveredIndex >= 0 && dragIndex < 0) {
            String name = stripColor(contacts.get(hoveredIndex).name());
            if (autoAnswer.contains(name)) {
                autoAnswer.remove(name);
            } else {
                autoAnswer.add(name);
            }
            saveSettings();
            return true;
        }

        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && shiftDown && hoveredIndex >= 0 && dragIndex < 0) {
            String name = stripColor(contacts.get(hoveredIndex).name());
            if (favorites.contains(name)) {
                favorites.remove(name);
            } else {
                favorites.add(name);
            }
            saveSettings();
            return true;
        }

        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && hoveredIndex >= 0 && dragIndex < 0) {
            dragIndex = hoveredIndex;
            dragStarted = false;
            dragStartX = mx;
            dragStartY = my;
            int row = hoveredIndex / cols;
            int col = hoveredIndex % cols;
            int itemX = cachedGridStartX + col * (cachedEffectiveSlot + cachedColGap);
            int itemY = gridTop + row * rowHeight - (int)scrollOffset;
            dragPickOffsetX = mx - itemX;
            dragPickOffsetY = my - itemY;
            dragMouseX = mx;
            dragMouseY = my;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragIndex >= 0) {
            dragMouseX = event.x();
            dragMouseY = event.y();
            double dx = dragMouseX - dragStartX;
            double dy = dragMouseY - dragStartY;
            if (!dragStarted && (dx * dx + dy * dy) > 25) {
                dragStarted = true;
            }
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragIndex >= 0) {
            if (dragStarted) {
                finishDrag();
            } else {
                AbiphoneTracker.ItemEntry entry = contacts.get(dragIndex);
                String rawName = stripColor(entry.name());
                var conn = Minecraft.getInstance().getConnection();
                if(contactsHasDiffNameInCMD.containsKey(rawName)) rawName = contactsHasDiffNameInCMD.get(rawName);
                if (conn != null) conn.sendCommand("call " + rawName);
                onClose();
            }
            dragIndex = -1;
            dragTargetIndex = -1;
            dragStarted = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void finishDrag() {
        if (dragIndex < 0) return;
        int target = dragTargetIndex >= 0 ? dragTargetIndex : dragIndex;
        if (target > dragIndex) target--;
        if (target != dragIndex) {
            AbiphoneTracker.ItemEntry moved = contacts.remove(dragIndex);
            contacts.add(target, moved);
            AbiphoneTracker.getInstance().saveOrderedItems(uuid, profileId, contacts);
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocused) {
            char c = (char) event.codepoint();
            if (c >= 32 && c != 127) {
                searchText += c;
                return true;
            }
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_LEFT_SHIFT || event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            shiftDown = true;
        }

        if (searchFocused) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
                return true;
            }
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_ESCAPE
            || Minecraft.getInstance().options.keyInventory.matches(event)) {
            if (dragIndex >= 0) { dragIndex = -1; dragTargetIndex = -1; return true; }
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_LEFT_SHIFT || event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            shiftDown = false;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ---- item creation ----

    private static List<Component> buildLore(AbiphoneTracker.ItemEntry entry) {
        List<Component> lore = new ArrayList<>();
        if (entry.description() != null && !entry.description().isEmpty()) {
            for (String line : entry.description().split("\n")) {
                lore.add(Component.literal(line).withStyle(ChatFormatting.GRAY)
                    .withStyle(style -> style.withItalic(false)));
            }
            lore.add(Component.empty());
        }
        lore.add(Component.translatable("babyzombieaddons.contact.lore.call")
            .withStyle(ChatFormatting.DARK_GRAY).withStyle(style -> style.withItalic(false)));
        lore.add(Component.translatable("babyzombieaddons.contact.lore.auto_answer")
            .withStyle(ChatFormatting.DARK_GRAY).withStyle(style -> style.withItalic(false)));
        lore.add(Component.translatable("babyzombieaddons.contact.lore.favorite")
            .withStyle(ChatFormatting.DARK_GRAY).withStyle(style -> style.withItalic(false)));
        lore.add(Component.translatable("babyzombieaddons.contact.lore.drag")
            .withStyle(ChatFormatting.DARK_GRAY).withStyle(style -> style.withItalic(false)));
        return lore;
    }

    private static ItemStack createItemStack(AbiphoneTracker.ItemEntry entry) {
        String displayName = stripColor(entry.name());
        Component nameComponent = Component.literal(displayName).withStyle(ChatFormatting.YELLOW)
            .withStyle(style -> style.withItalic(false));

        if (entry.nbt() != null) {
            try {
                var tag = TagParser.create(NbtOps.INSTANCE).parseFully(entry.nbt());
                var profile = ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
                if (profile != null) {
                    ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
                    stack.set(DataComponents.PROFILE, profile);
                    stack.set(DataComponents.CUSTOM_NAME, nameComponent);
                    stack.set(DataComponents.LORE, new ItemLore(buildLore(entry)));
                    return stack;
                }
            } catch (Exception ignored) {
            }
        }

        Identifier id = Identifier.tryParse(entry.material());
        ItemStack stack;
        if (id != null) {
            var item = BuiltInRegistries.ITEM.getValue(id);
            stack = item != Items.AIR ? new ItemStack(item) : new ItemStack(Items.BARRIER);
        } else {
            stack = new ItemStack(Items.BARRIER);
        }
        if (stack.getItem() != Items.AIR) {
            stack.set(DataComponents.CUSTOM_NAME, nameComponent);
        }
        stack.set(DataComponents.LORE, new ItemLore(buildLore(entry)));
        return stack;
    }

    private static String stripColor(String text) {
        return text.replaceAll("§[0-9a-fk-orlnm]", "");
    }
}
