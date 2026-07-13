package top.babyzombie.addons.module.loadout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.config.ModConfig.EntityRenderMode;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

import java.util.*;

public class LoadoutDisplayScreen extends Screen {
    private static final int[] EQUIP_SLOTS   = {10, 19, 28, 37};
    private static final int[] ARMOR_SLOTS   = {11, 20, 29, 38};
    private static final int[] PRESET_SLOTS  = {14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};
    private static final int PET_SLOT = 21, POWERSTONE_SLOT = 27, TUNINGS_SLOT = 36, HOTM_SLOT = 18, HOTF_SLOT = 9;
    private static final int PREV = 17, NEXT = 44, CLOSE = 49;
    private static final int COLS = 4, ROWS = 3;

    private final AbstractContainerScreen<?> parentContainer;
    private final Level clientLevel;
    private ItemStack[] slots;
    private final EntityRenderMode entityMode;

    /** 预设实体 + 解析后的装备/宠物 ItemStacks */
    private final LivingEntity[] presetEntities = new LivingEntity[12];
    private final LoadoutItemResolver.PresetData[] presetData = new LoadoutItemResolver.PresetData[12];
    /** 每个预设的装备 ItemStacks（4 饰品 + 1 宠物），用于图标渲染 */
    private final ItemStack[][] presetEquipIcons = new ItemStack[12][5]; // [equip0..3, pet]
    private boolean entitiesBuilt;

    private boolean layoutDirty = true;
    private int lm, lpw, gt, gb, cell, gap, gsx, gsy;

    private ItemStack hoveredItem;
    private int hp = -1, hls = -1, hbb = -1;

    private int frameCount = 0;

    public LoadoutDisplayScreen(AbstractContainerScreen<?> pc) {
        super(Component.literal("Loadouts"));
        this.parentContainer = pc;
        this.clientLevel = Minecraft.getInstance().level;
        this.entityMode = ModConfigManager.get().loadout.entityRenderMode;
        refreshSlots();
        // 延迟刷新（等服务器发物品）
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().execute(this::refreshSlots));
    }

    private void refreshSlots() {
        slots = new ItemStack[54];
        var ms = parentContainer.getMenu().slots;
        for (int i = 0; i < 54 && i < ms.size(); i++) slots[i] = ms.get(i).getItem().copy();
        entitiesBuilt = false;
        layoutDirty = true;
        top.babyzombie.addons.util.pet.PetManager.getInstance().scanLoadoutPet(parentContainer);
    }

    private void buildEntities() {
        if (entitiesBuilt || clientLevel == null) return;
        LoadoutItemResolver.ensureIndex();
        for (int i = 0; i < 12; i++) {
            ItemStack ps = slots[PRESET_SLOTS[i]];
            if (isEmpty(ps) || isGlassPane(ps)) continue;
            if (isLocked(ps)) continue; // 锁的跳过

            boolean empty = isEmptyPreset(ps);
            var data = LoadoutItemResolver.parsePresetLore(ps);
            presetData[i] = data;

            if (presetEntities[i] == null) {
                presetEntities[i] = switch (entityMode) {
                    case ARMOR_STAND -> { var a = new ArmorStand(clientLevel, 0, 0, 0); a.setNoBasePlate(true); yield a; }
                    default -> createSkinnedMannequin();
                };
            }
            if (!empty) equipEntity(presetEntities[i], i);

            // 解析装备图标
            String[] eq = data.getEquipmentNames();
            for (int e = 0; e < 4; e++) {
                presetEquipIcons[i][e] = ItemStack.EMPTY;
                if (eq[e] != null) {
                    String id = LoadoutItemResolver.getSkyblockId(eq[e]);
                    if (id != null) presetEquipIcons[i][e] = LoadoutItemResolver.createItemFromRepo(id);
                }
            }
            // 宠物图标
            presetEquipIcons[i][4] = ItemStack.EMPTY;
            if (data.petName != null) {
                String id = LoadoutItemResolver.getSkyblockId(data.petName);
                if (id != null) presetEquipIcons[i][4] = LoadoutItemResolver.createItemFromRepo(id);
            }
        }
        entitiesBuilt = true;
    }

    private void recalc() {
        lm = this.width / 40; // 小边距
        lpw = this.width * 25 / 100; // 左侧面板 25%
        int gl = lm + lpw + 8, gr = this.width - lm - 8;
        gt = 22; gb = this.height - 50;
        int gw = gr - gl, gh = gb - gt;
        gap = Math.max(3, gw / 60);
        cell = Math.min((gw - gap * (COLS - 1)) / COLS, (gh - gap * (ROWS - 1)) / ROWS);
        gsx = gl + (gw - (COLS * cell + (COLS - 1) * gap)) / 2;
        gsy = gt + (gh - (ROWS * cell + (ROWS - 1) * gap)) / 2;
        layoutDirty = false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (layoutDirty) recalc();
        // 前 60 帧密集刷新，之后每 20 帧检查
        ++frameCount;
        if ((frameCount < 60 && frameCount % 4 == 0) || (frameCount >= 60 && frameCount % 20 == 0)) {
            if (needsRefresh()) refreshSlots();
        }
        buildEntities();
        g.centeredText(this.font, pageTitle(), this.width / 2, 4, 0xFFFFFFFF);
        hoveredItem = null; hp = -1; hls = -1;
        renderLeft(g, mx, my);
        renderRight(g, mx, my);
        renderBottom(g, mx, my);
        if (hoveredItem != null) g.setTooltipForNextFrame(this.font, hoveredItem, mx, my);
    }

    // ==================== 左侧：ArmorStand 穿容器防具（非真实玩家） ====================

    private ClientMannequin leftEntity;

    private void renderLeft(GuiGraphicsExtractor g, int mx, int my) {
        int pl = lm, pr = lm + lpw;
        g.centeredText(this.font, "§e当前装备", (pl + pr) / 2, gt - 12, 0xFFFFFFFF);

        // 动态更新图标大小
        icSz = Math.max(18, this.width / 55);
        // 实体占面板主要空间
        int ew = lpw * 5 / 10;
        int eh = ew * 2;
        int ex = pl + (lpw - ew) / 2;
        int ey = gt + (gb - gt - eh) / 2;
        // 实体大小匹配面积（原版 30 → 49×70 面积）
        int entitySize = ew * 4 / 5;

        if (clientLevel != null) {
            if (leftEntity == null) leftEntity = createSkinnedMannequin();
            equipLeft();
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                g, ex, ey, ex + ew, ey + eh, entitySize, 0.0625f, (float) mx, (float) my, leftEntity);
        }
        // hover body → armor
        if (mx >= ex && mx < ex + ew && my >= ey && my < ey + eh) {
            int p = (my - ey) * 4 / eh;
            if (p >= 0 && p < 4) { ItemStack a = slots[ARMOR_SLOTS[p]]; if (!isEmpty(a)) { hoveredItem = a; hls = ARMOR_SLOTS[p]; } }
        }

        // 饰品 & 属性：上面图标 + 下面两行文字（垂直布局，文本更宽）
        // 左侧饰品（只显示图标，4 个均匀分布在实体高度内）
        int lx = ex - icSz - 2;
        int rowH = icSz + this.font.lineHeight * 2 + 4;
        int iconSp = Math.max(1, (eh - rowH * 5) / 6);
        int leftSpacing = (eh - icSz * 4) / 5;
        for (int i = 0; i < 4; i++) {
            int iy = ey + leftSpacing + i * (icSz + leftSpacing);
            ItemStack s = slots[EQUIP_SLOTS[i]];
            if (!isEmpty(s)) { renderItem(g, s, lx, iy); checkH(mx, my, lx, iy, icSz, icSz, EQUIP_SLOTS[i], s); }
            else renderEmpty(g, lx, iy, icSz);
        }
        // 右侧属性（图标 + 下面文字，最多 4 行）
        int rx = ex + ew + 2;
        int textW = pr - rx - 4;
        for (int i = 0; i < 5; i++) {
            int sid = new int[]{PET_SLOT, POWERSTONE_SLOT, TUNINGS_SLOT, HOTM_SLOT, HOTF_SLOT}[i];
            int iy = ey + iconSp + i * rowH;
            slotLineV(g, rx, iy, sid, mx, my, icSz, textW);
        }
    }

    /** 创建带本地玩家皮肤的 ClientMannequin（匿名子类 override getSkin，零反射） */
    private ClientMannequin createSkinnedMannequin() {
        var player = Minecraft.getInstance().player;
        if (player == null || clientLevel == null) return null;
        return new ClientMannequin(clientLevel, Minecraft.getInstance().playerSkinRenderCache()) {
            @Override
            public net.minecraft.world.entity.player.PlayerSkin getSkin() {
                return player.getSkin();
            }
        };
    }

    private void equipLeft() {
        if (leftEntity == null) return;
        EquipmentSlot[] es = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            leftEntity.setItemSlot(es[i], !isEmpty(slots[ARMOR_SLOTS[i]]) ? slots[ARMOR_SLOTS[i]].copy() : ItemStack.EMPTY);
        }
    }

    /** 垂直布局：上面图标，下面文字（宽区域可换行） */
    private void slotLineV(GuiGraphicsExtractor g, int x, int y, int sid, int mx, int my, int iconSz, int textW) {
        ItemStack s = slots[sid];
        if (!isEmpty(s)) {
            renderItem(g, s, x, y);
            checkH(mx, my, x, y, iconSz, iconSz, sid, s);
            String v = getProp(sid);
            String txt = v != null && !v.equalsIgnoreCase("None") ? v
                : ChatUtils.stripColor(s.getHoverName().getString());
            drawWrapped(g, Component.literal(txt).withColor(0xFF55FF55), x, y + iconSz, textW);
        } else {
            renderEmpty(g, x, y, iconSz);
            String v = getProp(sid);
            String txt = v != null && !v.equalsIgnoreCase("None") ? v : "-";
            drawWrapped(g, Component.literal(txt).withColor(0xFF888888), x, y + iconSz, textW);
        }
    }

    private void drawWrapped(GuiGraphicsExtractor g, Component text, int x, int y, int maxW) {
        var lines = this.font.split(text, maxW);
        int cy = y + 4;
        for (int i = 0; i < Math.min(lines.size(), 2); i++) {
            g.text(this.font, lines.get(i), x, cy, 0xFFFFFFFF, false);
            cy += this.font.lineHeight;
        }
    }
    private void drawWrappedRight(GuiGraphicsExtractor g, Component text, int rightX, int y, int maxW) {
        var lines = this.font.split(text, maxW);
        int cy = y + 4;
        int maxLines = 2; // 最多两行
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            var line = lines.get(i);
            int lw = this.font.width(line);
            g.text(this.font, line, rightX - lw, cy, 0xFFFFFFFF, false);
            cy += this.font.lineHeight;
        }
    }

    // ==================== 右侧面板 ====================

    private void renderRight(GuiGraphicsExtractor g, int mx, int my) {
        for (int i = 0; i < PRESET_SLOTS.length; i++) {
            int r = i / COLS, c = i % COLS;
            int cx = gsx + c * (cell + gap), cy = gsy + r * (cell + gap);
            int sid = PRESET_SLOTS[i];
            ItemStack ps = slots[sid];
            if (isEmpty(ps) || isGlassPane(ps)) continue;

            boolean lk = isLocked(ps), em = isEmptyPreset(ps), cur = !lk && !em && isCurrentPreset(ps);
            int bg = lk ? 0x40FF0000 : (cur ? 0x6000AA00 : (hp == i ? 0x60FFFFFF : 0x30FFFFFF));
            g.fill(cx, cy, cx + cell, cy + cell, bg);

            int m = 2, reh = cell - m * 2 - 14;
            int entityScale = cell / 3;
            LivingEntity re = presetEntities[i];
            boolean follow = entityMode == EntityRenderMode.FAKE_PLAYER_EYES;
            if (!lk && re != null) {
                // 实体放在中间偏上
                int ex = cx + cell / 4, ew = cell / 2;
                float emx = follow ? (float) mx : ex + ew / 2f;
                float emy = follow ? (float) my : cy + m + reh / 2f;
                InventoryScreen.extractEntityInInventoryFollowsMouse(
                    g, ex, cy + m, ex + ew, cy + m + reh, entityScale, 0.0625f, emx, emy, re);
            }

            // 左侧5个图标(饰品+宠物) + 右侧5行文本(统一间距)
            if (!lk && !em && presetData[i] != null) {
                var pd = presetData[i];
                int icSz2 = 10, lx = cx + 2, rx = cx + cell - 2;
                int GREEN = 0xFF55FF55, lh2 = this.font.lineHeight + 2;
                // 左侧图标：宠物在最上面，然后 4 饰品
                int iSp2 = Math.max(0, (reh - icSz2 * 5) / 6);
                int ty2 = cy + m + iSp2;
                if (!isEmpty(presetEquipIcons[i][4])) {
                    g.item(presetEquipIcons[i][4], lx, ty2);
                    ty2 += icSz2 + iSp2;
                }
                for (int e = 0; e < 4; e++) {
                    if (isEmpty(presetEquipIcons[i][e])) continue;
                    g.item(presetEquipIcons[i][e], lx, ty2);
                    ty2 += icSz2 + iSp2;
                }
                // 右侧5行文本统一 lh2 间距
                ty2 = cy + m;
                if (pd.petName != null && !pd.petName.equalsIgnoreCase("None") && ty2 < cy + reh) {
                    String d = pd.petLevel != null ? "Lv" + pd.petLevel + " " + pd.petName : pd.petName;
                    drawWrappedRight(g, Component.literal(d).withColor(0xFFFFAA00), rx, ty2, rx - lx);
                    ty2 += lh2;
                }
                if (pd.powerStone != null && !pd.powerStone.equalsIgnoreCase("None") && ty2 < cy + reh) {
                    drawWrappedRight(g, Component.literal("Power: " + pd.powerStone).withColor(GREEN), rx, ty2, rx - lx);
                    ty2 += lh2;
                }
                if (pd.tuningSlot != null && !pd.tuningSlot.equalsIgnoreCase("None") && ty2 < cy + reh) {
                    drawWrappedRight(g, Component.literal("Tuning: " + pd.tuningSlot).withColor(GREEN), rx, ty2, rx - lx);
                    ty2 += lh2;
                }
                if (pd.hotm != null && !pd.hotm.equalsIgnoreCase("None") && ty2 < cy + reh) {
                    drawWrappedRight(g, Component.literal("HOTM: " + pd.hotm).withColor(GREEN), rx, ty2, rx - lx);
                    ty2 += lh2;
                }
                if (pd.hotf != null && !pd.hotf.equalsIgnoreCase("None") && ty2 < cy + reh) {
                    drawWrappedRight(g, Component.literal("HOTF: " + pd.hotf).withColor(GREEN), rx, ty2, rx - lx);
                }
            }

            if (lk) g.centeredText(this.font, "§c§l🔒", cx + cell / 2, cy + 2, 0xFFFF5555);
            else if (em) g.centeredText(this.font, "§7空", cx + cell / 2, cy + 2, 0xFF888888);
            else if (cur) g.centeredText(this.font, "§a§l✔", cx + cell / 2, cy + 2, 0xFF55FF55);

            String nm = ChatUtils.stripColor(ps.getHoverName().getString());
            if (this.font.width(nm) > cell) nm = this.font.plainSubstrByWidth(nm, cell - 4) + "..";
            g.centeredText(this.font, nm, cx + cell / 2, cy + cell + 2, 0xFF55FF55);

            if (mx >= cx && mx < cx + cell && my >= cy && my < cy + cell) { hp = i; hoveredItem = ps; }
        }
    }

    private void equipEntity(LivingEntity entity, int idx) {
        ItemStack ps = slots[PRESET_SLOTS[idx]];
        var data = LoadoutItemResolver.parsePresetLore(ps);
        String[] names = data.getArmorNames();
        EquipmentSlot[] es = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            ItemStack armor = ItemStack.EMPTY;
            if (names[i] != null) {
                String id = LoadoutItemResolver.getSkyblockId(names[i]);
                if (id != null) armor = LoadoutItemResolver.createItemFromRepo(id);
            }
            entity.setItemSlot(es[i], armor);
        }
    }

    // ==================== 底部按钮 ====================

    private void renderBottom(GuiGraphicsExtractor g, int mx, int my) {
        int bh = 20, by = this.height - bh - 4, cx = this.width / 2, gb = 10;
        boolean hasPrev = !isEmpty(slots[PREV]) && !isGlassPane(slots[PREV]);
        boolean hasNext = !isEmpty(slots[NEXT]) && !isGlassPane(slots[NEXT]);
        String prev = Component.translatable("babyzombieaddons.loadout.prev_page").getString();
        String next = Component.translatable("babyzombieaddons.loadout.next_page").getString();
        String close = Component.translatable("babyzombieaddons.loadout.close").getString();
        String back = Component.translatable("babyzombieaddons.loadout.back").getString();
        int pw = this.font.width("  " + prev + "  ") + 8, cw = this.font.width("  " + close + "  ") + 8;
        int nw = this.font.width("  " + next + "  ") + 8, bw = this.font.width("  " + back + "  ") + 8;
        String pt = pageTitle().replaceAll("[^0-9/]", "");
        int ptw = this.font.width(pt);
        int totalW = (hasPrev ? pw + gb : 0) + ptw + gb + bw + gb + cw + gb + (hasNext ? nw : 0);
        int sx = cx - totalW / 2;
        hbb = -1; int x = sx;
        if (hasPrev) { btn(g, prev, x, by, pw, bh, mx, my, 0); x += pw + gb; }
        g.centeredText(this.font, pt, x + ptw / 2, by + bh / 2 - 6, 0x80FFFFFF); x += ptw + gb;
        btn(g, back, x, by, bw, bh, mx, my, 1); x += bw + gb;
        btn(g, close, x, by, cw, bh, mx, my, 2); x += cw + gb;
        if (hasNext) { btn(g, next, x, by, nw, bh, mx, my, 3); }
    }

    private void btn(GuiGraphicsExtractor g, String l, int x, int y, int w, int h, int mx, int my, int id) {
        if (mx >= x && mx < x + w && my >= y && my < y + h) hbb = id;
        g.fill(x, y, x + w, y + h, (hbb == id) ? 0x80FFFFFF : 0x40FFFFFF);
        g.centeredText(this.font, l, x + w / 2, y + h / 2 - 6, 0xFF8888FF);
    }

    // ==================== 输入 ====================

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        int btn = e.buttonInfo().button();
        if (hp >= 0) {
            int sid = PRESET_SLOTS[hp];
            ItemStack ps = slots[sid];
            // 锁/空/当前预设不响应左键
            if (!isLocked(ps) && !isEmptyPreset(ps) && !isCurrentPreset(ps)) {
                sendClick(sid, btn);
                if (minecraft != null) minecraft.execute(() -> minecraft.execute(this::refreshSlots));
            }
            return true;
        }
        if (hls >= 0) { sendClick(hls, btn); return true; }
        if (hbb >= 0) {
            switch (hbb) {
                case 0: sendClick(PREV, btn); if (minecraft != null) minecraft.execute(() -> minecraft.execute(this::refreshSlots)); break;
                case 1: returnToOrig(); break;
                case 2: sendClick(CLOSE, btn); doClose(); break;
                case 3: sendClick(NEXT, btn); if (minecraft != null) minecraft.execute(() -> minecraft.execute(this::refreshSlots)); break;
            }
            return true;
        }
        return super.mouseClicked(e, dbl);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (e.key() == GLFW.GLFW_KEY_ESCAPE) { doClose(); return true; }
        if (e.key() == GLFW.GLFW_KEY_E) { doClose(); return true; }
        // 1-9 → presets 1-9
        if (e.key() >= GLFW.GLFW_KEY_1 && e.key() <= GLFW.GLFW_KEY_9) {
            int idx = e.key() - GLFW.GLFW_KEY_1;
            clickPreset(idx); return true;
        }
        // 0 → preset 10
        if (e.key() == GLFW.GLFW_KEY_0) { clickPreset(9); return true; }
        // - → preset 11
        if (e.key() == GLFW.GLFW_KEY_MINUS) { clickPreset(10); return true; }
        // = → preset 12
        if (e.key() == GLFW.GLFW_KEY_EQUAL) { clickPreset(11); return true; }
        // W/A → 上一页
        if (e.key() == GLFW.GLFW_KEY_W || e.key() == GLFW.GLFW_KEY_A) {
            if (!isEmpty(slots[PREV]) && !isGlassPane(slots[PREV])) {
                sendClick(PREV, 0);
                if (minecraft != null) minecraft.execute(() -> minecraft.execute(this::refreshSlots));
            }
            return true;
        }
        // S/D → 下一页
        if (e.key() == GLFW.GLFW_KEY_S || e.key() == GLFW.GLFW_KEY_D) {
            if (!isEmpty(slots[NEXT]) && !isGlassPane(slots[NEXT])) {
                sendClick(NEXT, 0);
                if (minecraft != null) minecraft.execute(() -> minecraft.execute(this::refreshSlots));
            }
            return true;
        }
        return super.keyPressed(e);
    }

    private void clickPreset(int idx) {
        if (idx < 0 || idx >= PRESET_SLOTS.length) return;
        int sid = PRESET_SLOTS[idx];
        ItemStack ps = slots[sid];
        if (isEmpty(ps) || isGlassPane(ps) || isLocked(ps) || isEmptyPreset(ps) || isCurrentPreset(ps)) return;
        sendClick(sid, 0);
        if (minecraft != null) minecraft.execute(() -> minecraft.execute(this::refreshSlots));
    }

    private void sendClick(int slot, int btn) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return;
        if (isPresetSlot(slot))
            top.babyzombie.addons.util.pet.PetManager.getInstance().setLoadoutSwitchPending(true);
        minecraft.gameMode.handleContainerInput(parentContainer.getMenu().containerId, slot, btn,
            net.minecraft.world.inventory.ContainerInput.PICKUP, minecraft.player);
    }

    private boolean isPresetSlot(int slot) {
        for (int s : PRESET_SLOTS) if (s == slot) return true;
        return false;
    }

    private void returnToOrig() {
        LoadoutModule.closingGuard = 3;
        LoadoutModule.onCustomScreenClosed();
        if (minecraft != null) minecraft.setScreenAndShow(parentContainer);
    }

    private void doClose() {
        LoadoutModule.onCustomScreenClosed();
        if (minecraft != null && minecraft.player != null)
            minecraft.player.closeContainer();
    }

    @Override public void onClose() {
        LoadoutModule.onCustomScreenClosed();
        super.onClose();
    }
    @Override public boolean isPauseScreen() { return false; }

    // ==================== 工具 ====================

    private boolean needsRefresh() {
        // 关闭按钮(49)有物品 = 页面已加载完
        return isEmpty(slots[49]);
    }
    private String pageTitle() { return parentContainer != null ? parentContainer.getTitle().getString() : "Loadouts"; }
    private boolean isEmpty(ItemStack s) { return s == null || s.isEmpty() || s.is(Items.AIR); }
    private boolean isGlassPane(ItemStack s) { return !isEmpty(s) && BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().contains("glass_pane"); }
    private boolean isLocked(ItemStack s) {
        if (ChatUtils.stripColor(s.getHoverName().getString()).matches("Loadout \\d+ Locked")) return true;
        ItemLore lore = s.get(DataComponents.LORE);
        if (lore == null) return false;
        for (var l : lore.lines()) if (ChatUtils.stripColor(l.getString()).matches("Loadout \\d+ Locked")) return true;
        return false;
    }
    private boolean isEmptyPreset(ItemStack s) {
        ItemLore lore = s.get(DataComponents.LORE);
        if (lore == null) return false;
        for (var l : lore.lines()) if (ChatUtils.stripColor(l.getString()).contains("You must customize this loadout")) return true;
        return false;
    }
    private boolean isCurrentPreset(ItemStack s) {
        ItemLore lore = s.get(DataComponents.LORE);
        if (lore == null) return true;
        for (var l : lore.lines()) if (ChatUtils.stripColor(l.getString()).equals("Left-click to equip!")) return false;
        return true;
    }
    private void renderItem(GuiGraphicsExtractor g, ItemStack s, int x, int y) {
        var pose = g.pose();
        pose.pushMatrix();
        float scale = icSz / 16f;
        pose.scale(scale, scale);
        g.item(s, (int)(x / scale), (int)(y / scale));
        // decorations 按比例缩放后坐标
        g.itemDecorations(this.font, s, (int)(x / scale), (int)(y / scale));
        pose.popMatrix();
    }
    private int icSz = 20;
    private void renderEmpty(GuiGraphicsExtractor g, int x, int y, int sz) { g.fill(x, y, x + sz, y + sz, 0x20FFFFFF); }
    private void checkH(int mx, int my, int x, int y, int w, int h, int sid, ItemStack st) {
        if (mx >= x && mx < x + w && my >= y && my < y + h) { hls = sid; if (!isEmpty(st)) hoveredItem = st; }
    }
    private String getProp(int sid) {
        if (sid < 0 || sid >= slots.length) return null;
        ItemStack st = slots[sid]; if (isEmpty(st)) return null;
        ItemLore lore = st.get(DataComponents.LORE);
        if (lore != null) {
            for (var l : lore.lines()) {
                String s = ChatUtils.stripColor(l.getString());
                if (s.startsWith("Current: ")) return s.substring(9);
            }
            // Tuning 特殊：读 "Your tuning:" 下一行
            var all = new ArrayList<String>();
            lore.lines().forEach(l -> all.add(ChatUtils.stripColor(l.getString())));
            int idx = all.indexOf("Your tuning:");
            if (idx >= 0 && idx + 1 < all.size()) return all.get(idx + 1);
        }
        return ChatUtils.stripColor(st.getHoverName().getString());
    }
}
