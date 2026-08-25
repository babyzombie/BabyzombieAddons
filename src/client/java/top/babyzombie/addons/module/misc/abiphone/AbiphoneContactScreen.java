package top.babyzombie.addons.module.misc.abiphone;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
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
import com.mojang.blaze3d.platform.InputConstants;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.DataPersistence;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.*;

public class AbiphoneContactScreen extends Screen {

    /** UI 设置,全局存储于 data/abiphone_ui.json。 */
    private static final String SETTINGS_FILE = "abiphone_ui.json";

    // persisted
    private int textColor = 0xFFFFFFFF;
    private boolean colorBarVisible = true;
    private final Set<String> favorites = new HashSet<>();
    private final Set<String> autoAnswer = new HashSet<>();
    /** 联系人备注(显示名覆盖):key = 去色码的原名,value = 含 § 色码的备注。 */
    private final Map<String, String> notes = new LinkedHashMap<>();

    // search
    private final EditBox searchBox;
    private boolean shiftDown;
    private boolean searchFilterMode = true;

    // note inline edit (就地重命名,类似系统文件重命名)
    private int noteEditIndex = -1;
    private EditBox noteEditBox;

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
        searchBox = new EditBox(font, 0, 0, 0, 14, Component.empty());
        searchBox.setMaxLength(64);
        loadSettings();
    }

    private void loadSettings() {
        var json = DataPersistence.load(SETTINGS_FILE, UiSettings.class);
        if (json != null) {
            if (json.textColor != 0) textColor = json.textColor;
            colorBarVisible = json.colorBarVisible;
            if (json.favorites != null) favorites.addAll(json.favorites);
            if (json.autoAnswer != null) autoAnswer.addAll(json.autoAnswer);
            if (json.notes != null) notes.putAll(json.notes);
        }
    }

    private void saveSettings() {
        DataPersistence.save(SETTINGS_FILE, new UiSettings(textColor, colorBarVisible,
                new ArrayList<>(favorites), new ArrayList<>(autoAnswer), new LinkedHashMap<>(notes)));
    }

    public static Set<String> getAutoAnswerNames() {
        Set<String> result = new HashSet<>();
        var json = DataPersistence.load(SETTINGS_FILE, UiSettings.class);
        if (json != null && json.autoAnswer != null) result.addAll(json.autoAnswer);
        return result;
    }

    private static class UiSettings {
        int textColor;
        boolean colorBarVisible = true;
        List<String> favorites;
        List<String> autoAnswer;
        Map<String, String> notes;
        UiSettings(int c, boolean v, List<String> f, List<String> a, Map<String, String> n) {
            this.textColor = c; this.colorBarVisible = v; this.favorites = f; this.autoAnswer = a; this.notes = n;
        }
    }

    /** 联系人的显示名:有备注用备注(含 § 色码),否则用去色码的原名。 */
    private String displayNameOf(AbiphoneTracker.ItemEntry entry) {
        String key = stripColor(entry.name());
        String note = notes.get(key);
        return note != null && !note.isEmpty() ? note : key;
    }

    /** 按"可见字符"下标从可能含 § 码的字符串中截取子串,§ 码随所属字符段保留(可安全跨行换色)。 */
    private static String coloredPart(String colored, int startPlain, int endPlain) {
        StringBuilder sb = new StringBuilder();
        int plain = 0;
        for (int i = 0; i < colored.length(); i++) {
            char c = colored.charAt(i);
            if (c == '§' && i + 1 < colored.length()) {
                sb.append(c).append(colored.charAt(i + 1));
                i++;
                continue;
            }
            if (plain >= startPlain && plain < endPlain) sb.append(c);
            plain++;
        }
        return sb.toString();
    }

    /** 进入就地编辑:在目标卡片创建输入框,预填现有备注(& 形式回显)。 */
    private void startNoteEdit(int index) {
        if (index < 0 || index >= contacts.size()) return;
        noteEditIndex = index;
        String key = stripColor(contacts.get(index).name());
        noteEditBox = new EditBox(font, 0, 0, cachedEffectiveSlot, 14, Component.empty());
        noteEditBox.setMaxLength(48);
        noteEditBox.setHint(Component.translatable("babyzombieaddons.contact.note.hint"));
        noteEditBox.setValue(ChatUtils.sectionToAmp(notes.getOrDefault(key, "")));
        searchBox.setFocused(false);
        setFocused(noteEditBox);
    }

    /** 保存就地编辑的备注,空串/空白则删除备注;完成后退出编辑模式。 */
    private void saveNoteEdit() {
        if (noteEditIndex >= 0 && noteEditIndex < contacts.size() && noteEditBox != null) {
            String key = stripColor(contacts.get(noteEditIndex).name());
            String note = ChatUtils.ampToSection(noteEditBox.getValue().trim());
            if (note.isEmpty()) {
                notes.remove(key);
            } else {
                notes.put(key, note);
            }
            saveSettings();
        }
        cancelNoteEdit();
    }

    /** 取消就地编辑,不保存。 */
    private void cancelNoteEdit() {
        noteEditIndex = -1;
        noteEditBox = null;
        setFocused(null);
    }

    /** 判断鼠标是否落在正在就地编辑的卡片上(点击卡片保持编辑,点击其他位置取消)。
     *  与渲染循环一致:按搜索过滤后的可见顺序定位行列。 */
    private boolean clickOnNoteEditCard(double mx, double my) {
        if (noteEditIndex < 0) return false;
        String searchLower = searchBox.getValue().toLowerCase(Locale.ROOT);
        int vi = -1;
        for (int i = 0; i <= noteEditIndex && i < contacts.size(); i++) {
            String name = stripColor(displayNameOf(contacts.get(i)));
            if (searchFilterMode && !searchLower.isEmpty()
                && !name.toLowerCase(Locale.ROOT).contains(searchLower)) {
                continue;
            }
            vi++;
        }
        if (vi < 0) return false;
        int row = vi / cols;
        int col = vi % cols;
        int itemX = cachedGridStartX + col * (cachedEffectiveSlot + cachedColGap);
        int itemY = gridTop + row * rowHeight - (int)scrollOffset;
        return mx >= itemX && mx < itemX + cachedEffectiveSlot
            && my >= itemY && my < itemY + rowHeight;
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
        if (cols > contacts.size() && !contacts.isEmpty()) cols = contacts.size();
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

        int availWidth = gridRight - gridLeft;
        int colGap = 10;
        int actualCols = Math.max(3, (availWidth + colGap) / (slotSize + colGap));
        if (actualCols > contacts.size() && !contacts.isEmpty()) actualCols = Math.max(3, contacts.size());
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
        String searchLower = searchBox.getValue().toLowerCase(Locale.ROOT);

        for (int i = 0; i < contacts.size(); i++) {
            AbiphoneTracker.ItemEntry entry = contacts.get(i);
            String name = stripColor(displayNameOf(entry));
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
            String displayName = displayNameOf(entry);
            boolean isFavorite = favorites.contains(entryName);
            boolean isSearchMatch = !searchFilterMode && !searchLower.isEmpty()
                && stripColor(displayName).toLowerCase(Locale.ROOT).contains(searchLower);

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

            String name = displayName;
            if (name.isEmpty()) name = entry.name().replaceAll("§.", "");
            String plainName = stripColor(name);
            float maxWidth = effectiveSlot + 10;
            int nameX = itemX + effectiveSlot / 2;
            int textY = itemY + 2 + (int)(16 * iconScale) + 2;

            if (i == noteEditIndex && noteEditBox != null) {
                noteEditBox.setRectangle(effectiveSlot, 14, itemX, textY);
                noteEditBox.extractRenderState(gui, mouseX, mouseY, delta);
                gui.text(font, ChatUtils.ampToSection(noteEditBox.getValue()),
                    itemX + 3, textY + 15, 0xFFBBBBBB, false);
            } else if (font.width(plainName) > maxWidth) {
                String line1Plain = font.plainSubstrByWidth(plainName, (int)maxWidth);
                String remainderPlain = plainName.substring(line1Plain.length());
                String line2Plain = font.plainSubstrByWidth(remainderPlain, (int)maxWidth);
                boolean ellipsized = !line2Plain.equals(remainderPlain);
                if (ellipsized) line2Plain = line2Plain + "..";
                String line1 = coloredPart(name, 0, line1Plain.length());
                String line2 = coloredPart(name, line1Plain.length(), plainName.length());
                if (ellipsized) line2 = line2 + "..";
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

            if (notes.containsKey(entryName)) {
                String pen = "✎";
                int px = itemX + effectiveSlot - font.width(pen) - 3;
                int pyIconBottom = itemY + 2 + (int)(16 * iconScale);
                gui.text(font, pen, px, pyIconBottom - font.lineHeight, 0xFF55FFAA, false);
            }
        }

        if (dragStarted && dragIndex >= 0 && dragIndex < contacts.size()) {
            AbiphoneTracker.ItemEntry entry = contacts.get(dragIndex);
            ItemStack dragStack = createItemStack(entry, displayNameOf(entry));
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

        if (dragIndex < 0 && hoveredIndex >= 0 && hoveredIndex < contacts.size() && noteEditIndex < 0) {
            AbiphoneTracker.ItemEntry hovered = contacts.get(hoveredIndex);
            gui.setTooltipForNextFrame(font, createItemStack(hovered, displayNameOf(hovered)), mouseX, mouseY);
        }

        renderPanel(gui, mouseX, mouseY, delta);

        gui.centeredText(font, "Abiphone Contacts (" + contacts.size() + ")", width / 2, 8, 0xFFFFFF);

        if (noteEditIndex >= 0) {
            gui.centeredText(font, Component.translatable("babyzombieaddons.contact.note.help").getString(),
                width / 2, 20, 0x666666);
        }
    }

    // ---- left panel ----

    private void renderPanel(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
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
        searchBox.setRectangle(sbW, sbH, sbX, sbY);
        searchBox.extractRenderState(gui, mouseX, mouseY, delta);
    }

    private boolean panelClick(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
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
            searchBox.setFocused(true);
            setFocused(searchBox);
            searchBox.mouseClicked(event, doubleClick);
            return true;
        } else {
            searchBox.setFocused(false);
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

        if (noteEditIndex >= 0) {
            // 就地编辑模式:左键点击卡片保持编辑(可定位光标),点击其他位置取消;右键忽略
            if (btn == InputConstants.MOUSE_BUTTON_LEFT) {
                if (clickOnNoteEditCard(mx, my)) {
                    noteEditBox.mouseClicked(event, doubleClick);
                } else {
                    cancelNoteEdit();
                }
            }
            return true;
        }

        if (btn == InputConstants.MOUSE_BUTTON_LEFT && mx < panelWidth) {
            return panelClick(event, doubleClick);
        }

        if (btn == InputConstants.MOUSE_BUTTON_LEFT && mx >= panelWidth) {
            searchBox.setFocused(false);
        }

        if (btn == InputConstants.MOUSE_BUTTON_RIGHT && shiftDown && hoveredIndex >= 0 && dragIndex < 0) {
            startNoteEdit(hoveredIndex);
            return true;
        }

        if (btn == InputConstants.MOUSE_BUTTON_RIGHT && hoveredIndex >= 0 && dragIndex < 0) {
            String name = stripColor(contacts.get(hoveredIndex).name());
            if (autoAnswer.contains(name)) {
                autoAnswer.remove(name);
            } else {
                autoAnswer.add(name);
            }
            saveSettings();
            return true;
        }

        if (btn == InputConstants.MOUSE_BUTTON_LEFT && shiftDown && hoveredIndex >= 0 && dragIndex < 0) {
            String name = stripColor(contacts.get(hoveredIndex).name());
            if (favorites.contains(name)) {
                favorites.remove(name);
            } else {
                favorites.add(name);
            }
            saveSettings();
            return true;
        }

        if (btn == InputConstants.MOUSE_BUTTON_LEFT && hoveredIndex >= 0 && dragIndex < 0) {
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
        // 键盘/字符事件由容器焦点路由到当前 EditBox(noteEditBox 或 searchBox)
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_LSHIFT || event.key() == InputConstants.KEY_RSHIFT) {
            shiftDown = true;
        }

        if (noteEditIndex >= 0) {
            if (event.key() == InputConstants.KEY_ESCAPE) {
                cancelNoteEdit();
                return true;
            }
            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                saveNoteEdit();
                return true;
            }
            if (Minecraft.getInstance().options.keyInventory.matches(event)) return true;
            return super.keyPressed(event);
        }

        if (searchBox.isFocused()) {
            if (event.key() == InputConstants.KEY_ESCAPE) {
                searchBox.setFocused(false);
                return true;
            }
            if (Minecraft.getInstance().options.keyInventory.matches(event)) return true;
            return super.keyPressed(event);
        }

        if (event.key() == InputConstants.KEY_ESCAPE
            || Minecraft.getInstance().options.keyInventory.matches(event)) {
            if (dragIndex >= 0) { dragIndex = -1; dragTargetIndex = -1; return true; }
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (event.key() == InputConstants.KEY_LSHIFT || event.key() == InputConstants.KEY_RSHIFT) {
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
        lore.add(Component.translatable("babyzombieaddons.contact.lore.note")
            .withStyle(ChatFormatting.DARK_GRAY).withStyle(style -> style.withItalic(false)));
        lore.add(Component.translatable("babyzombieaddons.contact.lore.drag")
            .withStyle(ChatFormatting.DARK_GRAY).withStyle(style -> style.withItalic(false)));
        return lore;
    }

    private static ItemStack createItemStack(AbiphoneTracker.ItemEntry entry) {
        return createItemStack(entry, stripColor(entry.name()));
    }

    private static ItemStack createItemStack(AbiphoneTracker.ItemEntry entry, String displayName) {
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
