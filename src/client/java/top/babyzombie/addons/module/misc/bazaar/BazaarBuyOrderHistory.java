package top.babyzombie.addons.module.misc.bazaar;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemLore;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.SkyblockConfig.BazzarTopOrders.SignQuickAmount;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.mixin.screen.AbstractSignEditScreenAccessor;
import top.babyzombie.addons.mixin.screen.ScreenInvoker;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.DataPersistence;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.gui.overlay.ClickableText;
import top.babyzombie.addons.util.gui.overlay.GuiOverlayManager;
import top.babyzombie.addons.util.gui.overlay.IGuiOverlay;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BazaarBuyOrderHistory implements IGuiOverlay {

    public static final String HUD_NAME = "BazzarBuyOrderHistory";

    private static final BazaarBuyOrderHistory INSTANCE = new BazaarBuyOrderHistory();
    private static final Pattern REFUND_NUM = Pattern.compile(
            "You will be refunded .*? coins from ([\\d,]+)x missing items\\.?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern REFUND_NUM_LOOSE = Pattern.compile(
            "([\\d,]+)x missing", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUY_PREFIX_INTERLEAVED = Pattern.compile(
            "(?i)(?:§[0-9a-fk-orx])*B(?:§[0-9a-fk-orx])*U(?:§[0-9a-fk-orx])*Y(?:§[0-9a-fk-orx])*\\s+");

    /** 告示牌箭头行(全字匹配) */
    private static final String SIGN_CARET_LINE = "^^^^^^^^^^^^^^^";

    /** 剪贴板数量白名单:只允许数字、括号、运算符(含 x)、k/m/b、点、逗号、空格,且至少含一个数字 */
    private static final Pattern AMOUNT_EXPRESSION = Pattern.compile(
            "(?i)^(?=.*\\d)[0-9(),.kmb+\\-*/x\\s]+$");

    private static final List<HistoryEntry> history = new CopyOnWriteArrayList<>();
    @Nullable
    private static String loadedProfileKey;
    private static String pendingColored = null;
    private static String pendingPlain = null;
    private static long pendingTs = 0L;

    private static List<ClickableText> texts = List.of();
    private static long lastBuildMs = 0L;
    private static int lastSize = -1;
    private static int lastMaxLines = -1;
    private static String lastProfileKey = "";
    /** 历史内容只会在"新增/换档加载"时变化，渲染重建由签名驱动，不用无条件定时器 */
    private static final long REBUILD_FALLBACK_MS = 2000L;
    /** 历史硬上限：与 buyOrderHistoryMaxLines 滑块上限（1-30）一致，超过显示上限的条目不会有用 */
    private static final int HARD_MAX_ENTRIES = 30;

    private BazaarBuyOrderHistory() {}

    public static void init() {
        GuiOverlayManager.register(INSTANCE);
        ContainerClickEvents.BEFORE_MOUSE_CLICK.register(INSTANCE::onSlotClick);
        // 告示牌数量自动填入:打开 SignEditScreen 时按内容匹配 Bazaar 输入数量布局
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            tryPasteClipboardAmount(screen);
            tryAddQuickAmountButtons(screen);
        });
    }

    // ========== 配置 ==========

    private static top.babyzombie.addons.config.SkyblockConfig.BazzarTopOrders getCfg() {
        try { return ModConfigManager.get().skyblock.bazzarTopOrders; } catch (Exception e) { return null; }
    }

    private static boolean enabled() {
        var c = getCfg();
        return c != null && c.buyOrderHistoryEnabled;
    }

    // ========== UUID+档案 持久化（参考 loadout 缓存） ==========

    private record PersistedHistory(List<HistoryEntry> entries) {}

    @Nullable
    private static String profileKey() {
        var t = HypixelLocationTracker.getInstance();
        String uuid = t.getUuid();
        String profileId = t.getProfileId();
        if (uuid == null || profileId == null) return null;
        return uuid + "/" + profileId;
    }

    /** 从磁盘按 uuid/profileId 加载历史，同档案不重复读 */
    public static void loadFromDiskIfNeeded() {
        if (HypixelLocationTracker.getInstance().isInAlpha()) return;
        String key = profileKey();
        if (key == null) return;
        if (key.equals(loadedProfileKey)) return;
        loadedProfileKey = key;
        PersistedHistory data = DataPersistence.load(key, "bazaar_buy_history.json", PersistedHistory.class);
        history.clear();
        if (data != null && data.entries != null) {
            int cap = Math.min(data.entries.size(), HARD_MAX_ENTRIES);
            for (int i = 0; i < cap; i++) history.add(data.entries.get(i));
        }
        rebuildTexts();
    }

    /** 立即按当前 uuid/profileId 将 history 写入磁盘（按硬上限裁掉尾部） */
    private static void saveToDisk() {
        if (HypixelLocationTracker.getInstance().isInAlpha()) return;
        String key = profileKey();
        if (key == null) return;
        loadedProfileKey = key;
        while (history.size() > HARD_MAX_ENTRIES) history.removeLast();
        DataPersistence.save(key, "bazaar_buy_history.json", new PersistedHistory(new ArrayList<>(history)));
    }

    // ========== IGuiOverlay ==========

    @Override public boolean shouldRender(Screen screen) {
        if (!enabled()) return false;
        loadFromDiskIfNeeded();
        if (history.isEmpty()) return false;
        return isOrdersOrOptionsScreen(screen);
    }

    private static boolean isOrdersOrOptionsScreen(Screen screen) {
        if (screen == null) return false;
        String t = ChatUtils.stripColor(screen.getTitle().getString()).trim();
        return "Bazaar Orders".equals(t) || "Co-op Bazaar Orders".equals(t) || "Order options".equals(t);
    }

    @Override public void onInventoryUpdated() { rebuildTexts(); }

    @Override
    public void render(GuiGraphicsExtractor g, int mx, int my, float delta) {
        var cfg = getCfg();
        if (cfg == null) return;
        long now = System.currentTimeMillis();
        int curSize = history.size();
        // 签名驱动：大小/行数上限/档案 key 变化或 2s 兜底时才重建（替换原无条件 500ms 定时重建）
        if (curSize != lastSize || lastMaxLines != cfg.buyOrderHistoryMaxLines
                || !Objects.equals(loadedProfileKey, lastProfileKey)
                || now - lastBuildMs > REBUILD_FALLBACK_MS) {
            lastSize = curSize; lastMaxLines = cfg.buyOrderHistoryMaxLines;
            lastProfileKey = loadedProfileKey == null ? "" : loadedProfileKey;
            lastBuildMs = now;
            rebuildTexts();
        }
        Font font = Minecraft.getInstance().font;
        int x = HudManager.x(HUD_NAME);
        int y = HudManager.y(HUD_NAME);
        float s = HudManager.scale(HUD_NAME);
        for (ClickableText t : texts) t.render(g, font, x, y, s, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        Font font = Minecraft.getInstance().font;
        int x = HudManager.x(HUD_NAME); int y = HudManager.y(HUD_NAME);
        float s = HudManager.scale(HUD_NAME);
        for (ClickableText t : texts) {
            if (t.hitTest((int)mx, (int)my, x, y, s, font)) {
                if (t.onClickLeft != null) { t.click(); return true; }
            }
        }
        return false;
    }

    // ========== GUI 点击监听 ==========

    private boolean onSlotClick(AbstractContainerScreen<?> screen, Slot slot, MouseButtonEvent event) {
        if (!enabled()) return false;
        if (event.button() != 0) return false;
        if (slot == null || !slot.hasItem()) return false;
        ItemStack stack = slot.getItem();

        String title = ChatUtils.stripColor(screen.getTitle().getString()).trim();

        // ---- 订单页：BUY 物品点击 ----
        if (isOrdersPageTitle(title)) {
            // 每次进入订单页，超过 60s 的 pending 自动丢弃（防残留）
            if (pendingTs > 0 && System.currentTimeMillis() - pendingTs > 60_000L) {
                pendingColored = null; pendingPlain = null; pendingTs = 0L;
            }
            handleOrdersPageBuy(stack);
            return false;
        }

        // ---- Order options 二级页：Cancel Order 点击 ----
        if ("Order options".equals(title)) {
            handleOrderOptionsCancel(stack);
            return false;
        }

        return false;
    }

    private static boolean isOrdersPageTitle(String t) {
        return "Bazaar Orders".equals(t) || "Co-op Bazaar Orders".equals(t);
    }

    private static void handleOrdersPageBuy(ItemStack stack) {
        String nameRaw = ChatUtils.stripColor(stack.getHoverName().getString());
        String name = nameRaw.trim();
        if (!name.toUpperCase(Locale.ROOT).startsWith("BUY ")) return;
        String plain = name.substring(4).trim();
        if (plain.isEmpty()) return;

        List<String> lore = getCleanedLore(stack);
        boolean hasClickHint = false;
        for (String l : lore) {
            if ("Click to view options!".equals(l)) { hasClickHint = true; break; }
        }
        if (!hasClickHint) return;

        String legacy = ChatUtils.toLegacyString(stack.getDisplayName());
        String colored = BUY_PREFIX_INTERLEAVED.matcher(legacy).replaceFirst("");
        if (colored.isEmpty()) colored = plain; // 兜底

        pendingPlain = plain;
        pendingColored = colored;
        pendingTs = System.currentTimeMillis();
    }

    private static void handleOrderOptionsCancel(ItemStack stack) {
        String nameRaw = ChatUtils.stripColor(stack.getHoverName().getString());
        if (!"Cancel Order".equals(nameRaw.trim())) return;

        if (pendingPlain == null || pendingColored == null) return;

        List<String> lore = getCleanedLore(stack);
        Integer amount = parseRefundAmount(lore);
        if (amount == null || amount <= 0) return;

        // 改成 loadout 同款"仅从缓存 json 读"：不直接往内存 history 插入
        // 1) 先从磁盘读出旧数据（若 profile key 同内存 loadedProfileKey 则直接复用内存）
        String key = profileKey();
        List<HistoryEntry> entries;
        if (key != null && key.equals(loadedProfileKey)) {
            entries = new ArrayList<>(history);
        } else {
            List<HistoryEntry> loaded = null;
            if (key != null) {
                PersistedHistory data = DataPersistence.load(key, "bazaar_buy_history.json", PersistedHistory.class);
                if (data != null && data.entries != null) loaded = data.entries;
            }
            entries = loaded != null ? new ArrayList<>(loaded) : new ArrayList<>();
        }
        // 2) 头插 + 100 条上限裁剪
        entries.addFirst(new HistoryEntry(pendingColored, pendingPlain, amount));
        while (entries.size() > HARD_MAX_ENTRIES) entries.removeLast();
        // 3) 写磁盘（DataPersistence.save 原子写，参考 loadout）
        if (key != null) DataPersistence.save(key, "bazaar_buy_history.json", new PersistedHistory(entries));
        // 4) 标记需在下一次 shouldRender 时从磁盘重载（强制 reload，使 reload 只走 json）
        loadedProfileKey = null;
        pendingColored = null; pendingPlain = null; pendingTs = 0L;
        rebuildTexts();
    }

    private static Integer parseRefundAmount(List<String> lore) {
        for (String line : lore) {
            if (line == null) continue;
            Matcher m = REFUND_NUM.matcher(line);
            if (m.find()) return parseInt(m.group(1));
        }
        for (String line : lore) {
            if (line == null) continue;
            Matcher m = REFUND_NUM_LOOSE.matcher(line);
            if (m.find()) return parseInt(m.group(1));
        }
        ChatUtils.showMessage("§c[BZA] 解析 Cancel Order 数量失败，请反馈 Lore 内容。");
        return null;
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.replace(",", "")); }
        catch (NumberFormatException e) { return -1; }
    }

    /** 获得 PUA/Emoji 清洗 + stripColor + trim 的纯文本 Lore 列表（含 DisplayName 后的所有行） */
    private static List<String> getCleanedLore(ItemStack stack) {
        List<String> out = new ArrayList<>();
        if (stack == null) return out;
        ItemLore itemLore = stack.get(DataComponents.LORE);
        if (itemLore != null) {
            for (Component c : itemLore.lines()) {
                String s = ChatUtils.stripColor(ChatUtils.removeEmoji(ChatUtils.toLegacyString(c))).trim();
                if (!s.isEmpty()) out.add(s);
            }
        }
        if (out.isEmpty()) {
            try {
                var mc = Minecraft.getInstance();
                var ctx = mc.level != null
                        ? net.minecraft.world.item.Item.TooltipContext.of(mc.level)
                        : net.minecraft.world.item.Item.TooltipContext.EMPTY;
                List<Component> lines = stack.getTooltipLines(ctx, mc.player, TooltipFlag.Default.NORMAL);
                for (Component c : lines) {
                    String s = ChatUtils.stripColor(ChatUtils.removeEmoji(ChatUtils.toLegacyString(c))).trim();
                    if (!s.isEmpty()) out.add(s);
                }
            } catch (Exception ignored) {}
        }
        return out;
    }

    // ========== HUD 渲染构建 ==========

    private static void rebuildTexts() {
        var cfg = getCfg();
        if (cfg == null) { texts = List.of(); return; }
        List<ClickableText> out = new ArrayList<>();
        if (history.isEmpty()) { texts = out; return; }

        Font font = Minecraft.getInstance().font;
        int lineH = font.lineHeight + 2;
        int curY = 0;
        int maxLines = Math.max(1, cfg.buyOrderHistoryMaxLines);
        int limit = Math.min(maxLines, history.size());

        String titleText = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.buyOrderHistoryTitle");
        out.add(new ClickableText(0, curY, titleText, 0xFFFFFFFF, List.of(), null));
        curY += lineH;

        // 历史 HUD 改为单入口交互：点击物品名时同时复制数量并跳转 Bazaar。
        // 因此数字不再拥有独立热点，只保留可见文本。
        for (int i = 0; i < limit; i++) {
            HistoryEntry e = history.get(i);
            int idx = i + 1;

            String prefix = "§7" + idx + ". ";
            String amtStr = String.valueOf(e.amount);
            String suffixLabel = " §7x ";
            String qtyText = "§a" + amtStr;
            String fullLine = prefix + e.coloredItemName + suffixLabel + qtyText;

            int itemRelX = ClickableText.measureWidth(font, prefix);

            // 可见底行
            out.add(new ClickableText(0, curY, fullLine, 0xFFFFFFFF, List.of(), null));

            // 物品名热点：复制数量 + 跳转 Bazaar
            final String plainItem = e.plainItemName;
            final int amount = e.amount;
            out.add(new ClickableText(itemRelX, curY, e.coloredItemName, 0x00FFFFFF,
                    List.of("config.babyzombieaddons.overlay.bazzar.tooltip.buyHistoryJump"),
                    () -> jumpToItem(plainItem, amount)));

            curY += lineH;
        }
        texts = out;
    }

    private static void jumpToItem(String plainItem, int amount) {
        ChatUtils.copyToClipboard(String.valueOf(amount));
        ChatUtils.showToast("config.babyzombieaddons.overlay.bazzar.toast.copiedTitle",
                "config.babyzombieaddons.overlay.bazzar.toast.buyHistoryJumpBody", amount);
        var mc = Minecraft.getInstance();
        try {
            if (mc.player != null) mc.player.closeContainer();
        } catch (Exception ignored) {}
        playClickSound();
        Scheduler.schedule(2, () -> ChatUtils.sendCommand("bz " + plainItem));
    }

    // ========== 告示牌自动填入剪贴板数量 ==========

    private static void tryPasteClipboardAmount(Screen screen) {
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
        var cfg = getCfg();
        if (cfg == null || !cfg.signPasteAmount) return;
        if (!(screen instanceof SignEditScreen signScreen)) return;
        String amount = readClipboardAmount();
        if (amount == null) return;
        // 告示牌行内容可能比屏幕打开晚到(服务端先开屏再同步 NBT),延迟重试几次等待数据
        Scheduler.schedule(0, new SignPasteTask(signScreen, amount));
    }

    /** 读取剪贴板并校验:只含数字、括号、运算符(含 x)、k/m/b、点、逗号等数量相关字符 */
    @Nullable
    private static String readClipboardAmount() {
        String clip;
        try {
            clip = Minecraft.getInstance().keyboardHandler.getClipboard();
        } catch (Exception e) {
            return null;
        }
        String amount = ChatUtils.stripColor(clip).trim();
        if (amount.isEmpty() || amount.length() > 32) return null;
        return AMOUNT_EXPRESSION.matcher(amount).matches() ? amount : null;
    }

    /** 布局匹配(只看告示牌内容)+ 写入第一行;写入成功返回 true */
    private static boolean pasteAmountIntoSign(SignEditScreen screen, String amount) {
        String[] messages = ((AbstractSignEditScreenAccessor) screen).messages();
        if (!isAmountSignLayout(messages)) return false;
        messages[0] = amount; // 空行变成文字即为可见反馈,无需 toast
        return true;
    }

    /** 匹配 Bazaar 输入数量的告示牌布局:第一行空 / ^^^ 箭头行 / Enter amount / to order 或 to sell(三行全字匹配) */
    private static boolean isAmountSignLayout(String[] messages) {
        if (messages.length < 4) return false;
        if (!lineOf(messages[0]).isEmpty()) return false;
        if (!SIGN_CARET_LINE.equals(lineOf(messages[1]))) return false;
        if (!"Enter amount".equals(lineOf(messages[2]))) return false;
        String last = lineOf(messages[3]);
        return "to order".equals(last) || "to sell".equals(last);
    }

    private static String lineOf(String s) {
        return s == null ? "" : ChatUtils.stripColor(s).trim();
    }

    /** 告示牌行内容比屏幕打开晚到时,带最大尝试次数的重试粘贴任务 */
    private static final class SignPasteTask implements Runnable {
        private static final int MAX_ATTEMPTS = 5;
        private final SignEditScreen screen;
        private final String amount;
        private int attempts;

        SignPasteTask(SignEditScreen screen, String amount) {
            this.screen = screen;
            this.amount = amount;
        }

        @Override
        public void run() {
            if (Minecraft.getInstance().gui.screen() != screen) return; // 屏幕已关闭/切换,放弃
            if (pasteAmountIntoSign(screen, amount)) return;      // 已填入,结束
            if (++attempts < MAX_ATTEMPTS) Scheduler.schedule(3, this); // 行内容未到,稍后重试
        }
    }

    // ========== 告示牌快捷数量按钮 ==========

    /** 已安装到当前屏幕的按钮引用,用于移除残留(窗口 resize 重 init 等场景) */
    private static final List<Button> quickButtons = new ArrayList<>();

    /** 26.1 起 Screen 的 children(事件)/renderables(渲染)/narratables 是三个独立列表,
     *  只改 children 会"点得动但看不见",必须走 ScreenInvoker 同步加入三个列表 */
    private static void addButtonToScreen(Screen screen, Button button) {
        ((ScreenInvoker) screen).bzaAddRenderableWidget(button);
    }

    private static void removeButtonFromScreen(Screen screen, Button button) {
        ((ScreenInvoker) screen).bzaRemoveWidget(button);
    }

    private static void tryAddQuickAmountButtons(Screen screen) {
        var cfg = getCfg();
        if (cfg == null || !cfg.signQuickAmountsEnabled) return;
        if (!(screen instanceof SignEditScreen signScreen)) return;
        if (cfg.signQuickAmounts == null || cfg.signQuickAmounts.isEmpty()) return;
        // 与剪贴板贴入同节奏:告示牌行内容可能比屏幕打开晚到,延迟重试等待布局可判
        Scheduler.schedule(0, new QuickButtonsTask(signScreen, new ArrayList<>(cfg.signQuickAmounts)));
    }

    /** 布局匹配后在告示牌下方安装一排快捷按钮(最多两行,每行居中);成功返回 true */
    private static boolean installQuickButtons(SignEditScreen screen, List<SignQuickAmount> amounts) {
        String[] messages = ((AbstractSignEditScreenAccessor) screen).messages();
        if (!isAmountSignLayout(messages)) return false;

        // 移除上次可能残留的按钮,保证始终只有一排
        for (Button old : new ArrayList<>(quickButtons)) removeButtonFromScreen(screen, old);
        quickButtons.clear();

        Font font = Minecraft.getInstance().font;
        int btnH = 20;
        int gap = 4;
        int baseY = 174; // 告示牌(66~168)正下方
        int maxRows = 2;
        // 一排最大宽度限制在屏宽与 320 的较小值,按钮集中在画面中部,不会横贯整个屏幕
        int maxRowWidth = Math.min(screen.width - 16, 320);
        int row = 0;
        int yPos = baseY;
        List<Button> fresh = new ArrayList<>();
        int i = 0;
        while (i < amounts.size() && row < maxRows) {
            // 逐行填装:统计这一行能放下的按钮
            int rowW = -gap;
            int count = 0;
            int start = i;
            while (i < amounts.size()) {
                int w = buttonWidth(font, amounts.get(i));
                if (count > 0 && rowW + gap + w > maxRowWidth) break;
                rowW += gap + w;
                count++;
                i++;
            }
            int startX = screen.width / 2 - rowW / 2;
            for (int j = start; j < i; j++) {
                final int amount = amounts.get(j).amount();
                int w = buttonWidth(font, amounts.get(j));
                Button b = Button.builder(Component.literal(amounts.get(j).toString()),
                        _ -> onQuickAmountClick(screen, amount))
                        .bounds(startX, yPos, w, btnH)
                        .build();
                fresh.add(b);
                startX += w + gap;
            }
            row++;
            yPos = baseY + row * (btnH + gap);
        }
        fresh.forEach(b -> addButtonToScreen(screen, b));
        quickButtons.addAll(fresh);
        return true;
    }

    private static int buttonWidth(Font font, SignQuickAmount o) {
        return Math.max(36, font.width(o.toString()) + 14);
    }

    private static void onQuickAmountClick(SignEditScreen screen, int amount) {
        ((AbstractSignEditScreenAccessor) screen).messages()[0] = String.valueOf(amount);
        playClickSound();
    }

    /** 告示牌行内容晚到时,带最大尝试次数的按钮安装重试任务 */
    private static final class QuickButtonsTask implements Runnable {
        private static final int MAX_ATTEMPTS = 5;
        private final SignEditScreen screen;
        private final List<SignQuickAmount> amounts;
        private int attempts;

        QuickButtonsTask(SignEditScreen screen, List<SignQuickAmount> amounts) {
            this.screen = screen;
            this.amounts = amounts;
        }

        @Override
        public void run() {
            if (Minecraft.getInstance().screen != screen) return; // 屏幕已关闭/切换,放弃
            if (installQuickButtons(screen, amounts)) return;     // 已安装,结束
            if (++attempts < MAX_ATTEMPTS) Scheduler.schedule(3, this); // 行内容未到,稍后重试
        }
    }

    // ========== 音效 ==========
    private static void playClickSound() {
        try {
            var p = Minecraft.getInstance().player;
            if (p != null) p.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.0f);
        } catch (Exception ignored) {}
    }
    // ========== 内部数据结构 ==========
    private static final class HistoryEntry {
        final String coloredItemName;
        final String plainItemName;
        final int amount;

        HistoryEntry(String colored, String plain, int amount) {
            this.coloredItemName = colored == null ? "" : colored;
            this.plainItemName = plain == null ? "" : plain;
            this.amount = amount;
        }
    }
}
