package top.babyzombie.addons.module.misc.bazaar;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.hud.HudTag;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.gui.overlay.*;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.tracker.BazaarItemInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bazzar 页面 Top Order 信息映射 + Flip 模式。
 * ChestCounter 模式 + IGuiOverlay 注册。
 */
public final class BazzarTopOrdersOverlay implements IGuiOverlay {
    public static final String HUD_BUY = "BazzarBuyOrder";
    public static final String HUD_SELL = "BazzarSellOrder";
    public static final String HUD_ACTION = "BazzarAction";

    private static final BazzarTopOrdersOverlay INSTANCE = new BazzarTopOrdersOverlay();
    private static final long REBUILD_INTERVAL_MS = 200L;
    private static final String PREFIX_BUY = "[Bazaar] Submitting buy order...";
    private static final String PREFIX_SELL = "[Bazaar] Submitting sell offer...";

    private static List<ClickableText> buyTexts = List.of();
    private static List<ClickableText> sellTexts = List.of();
    private static List<ClickableText> actionTexts = List.of();
    private static long lastRebuildMs = 0L;
    private static String lastParsedItemName = "";
    /** 订单页悬浮列表缓存：productId + 快照时间，命中时不逐帧重建 */
    private static String lastOrdersHoverKey = "";
    private static long lastOrdersHoverTs = -1L;

    private BazzarTopOrdersOverlay() {}

    public static void init() {
        GuiOverlayManager.register(INSTANCE);

        // Tick 节流重建（无缓存承诺：每次都完整重新 parse）
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            var cfg = getCfg();
            if (cfg == null || !cfg.overlayEnabled) return;
Screen s = Minecraft.getInstance().gui.screen();
            // Bazaar 界面（含列表页）就刷新数据，操作栏常显；订单数据仅详情页解析。
            // 订单页（单独开关）同样刷新数据（悬浮订单信息依赖 API）
            boolean bazaar = BazzarInventoryMatcher.isBazzarScreen(s);
            boolean orders = cfg.ordersPageEnabled && isOrdersPage(s);
            if (!bazaar && !orders) return;
            // API 模式下刷新数据（60s 节流由 tracker 保证）
            if (cfg.apiEnabled) BazaarItemInfo.ensureFresh();
            long now = System.currentTimeMillis();
            if (now - lastRebuildMs < REBUILD_INTERVAL_MS) return;
            lastRebuildMs = now;
            rebuildTexts();
        });

        // Flip 聊天监听：匹配 Submitting 开头的两条固定前缀即可
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            if (overlay) return;
            var cfg = getCfg();
            if (cfg == null || !cfg.flipEnabled) return;
            String text = ChatUtils.removeEmoji(ChatUtils.stripColor(msg.getString()));
            boolean buy = text.startsWith(PREFIX_BUY);
            boolean sell = !buy && text.startsWith(PREFIX_SELL);
            if (!buy && !sell) return;
            if (buy && !cfg.flipBuyEnabled) return;
            if (sell && !cfg.flipSellEnabled) return;
            String item = lastParsedItemName;
            if (item == null || item.isEmpty()) return;
            final String finalItem = item.trim();
            Scheduler.schedule(5, () -> ChatUtils.sendCommand("bz " + finalItem));
        });
    }

    private static top.babyzombie.addons.config.SkyblockConfig.BazzarTopOrders getCfg() {
        try { return ModConfigManager.get().skyblock.bazzarTopOrders; } catch (Exception e) { return null; }
    }

    // ========== IGuiOverlay ==========
    @Override public boolean shouldRender(Screen screen) {
        var cfg = getCfg();
        if (cfg == null || !cfg.overlayEnabled) return false;
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return false;
        if (BazzarInventoryMatcher.isBazzarScreen(screen)) return true;
        // 订单页：单独开关控制（悬浮订单信息 + 操作栏）
        return cfg.ordersPageEnabled && isOrdersPage(screen);
    }

    @Override public void onInventoryUpdated() { rebuildTexts(); }

    @Override
    public void render(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (!(Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> cs)) return;
        Font font = Minecraft.getInstance().font;

        if (isOrdersPage(cs)) {
            // 订单页：悬浮物品时用 API 数据填充两个订单 HUD（同详情页）；未悬浮保持空
            refreshOrdersPageLists(cs, mx, my);
            renderGroup(g, font, HUD_BUY, buyTexts, mx, my);
            renderGroup(g, font, HUD_SELL, sellTexts, mx, my);
            renderGroup(g, font, HUD_ACTION, actionTexts, mx, my);
            return;
        }

        if (buyTexts.isEmpty() && sellTexts.isEmpty() && actionTexts.isEmpty()) rebuildTexts();

        // Buy Order 组
        renderGroup(g, font, HUD_BUY, buyTexts, mx, my);
        // Sell Order 组
        renderGroup(g, font, HUD_SELL, sellTexts, mx, my);
        // Action 组
        renderGroup(g, font, HUD_ACTION, actionTexts, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        Font font = Minecraft.getInstance().font;
        if (hitGroup(HUD_BUY, buyTexts, mx, my, font)) return true;
        if (hitGroup(HUD_SELL, sellTexts, mx, my, font)) return true;
        if (hitGroup(HUD_ACTION, actionTexts, mx, my, font)) return true;
        return false;
    }

    // ========== helpers ==========
    private static void renderGroup(GuiGraphicsExtractor g, Font f, String hudName,
                                     List<ClickableText> list, int mx, int my) {
        int x = HudManager.x(hudName);
        int y = HudManager.y(hudName);
        float s = HudManager.scale(hudName);
        for (ClickableText t : list) t.render(g, f, x, y, s, mx, my);
    }

    private static boolean hitGroup(String hudName, List<ClickableText> list,
                                     double mx, double my, Font f) {
        int x = HudManager.x(hudName); int y = HudManager.y(hudName);
        float s = HudManager.scale(hudName);
        for (ClickableText t : list) {
            if (t.hitTest((int)mx, (int)my, x, y, s, f)) {
                // onClickLeft 为 null 的行不消费事件，让更小区域的 ClickableText 能被点击
                if (t.onClickLeft != null) { t.click(); return true; }
            }
        }
        return false;
    }

    // ========== 重建（每次必 parse，无缓存） ==========
    private static void rebuildTexts() {
        var cfg = getCfg();
        if (cfg == null) return;
        Screen s = Minecraft.getInstance().gui.screen();
        AbstractContainerScreen<?> cs = (s instanceof AbstractContainerScreen<?> a) ? a : null;

        // ===== 数据来源：API 优先（开关开启且数据就绪），否则 GUI 解析 =====
        List<TopOrderData.TopOrderEntry> buys = List.of();
        List<TopOrderData.TopOrderEntry> sells = List.of();
        String itemName = "";
        // 订单页的中间槽是普通订单物品，不是当前查看物品：不读中间槽，避免污染 flip 物品名
        ItemStack center = null;
        if (!isOrdersPage(cs)) {
            // 真实物品名：优先中间槽 ItemStack 显示名（游戏内权威名），数据源无关
            center = BazzarInventoryMatcher.getCenterItem(cs);
            if (center != null) {
                // 显示用名保留原色（如 §d§lCrop Fever V）
                String n = ChatUtils.toLegacyString(center.getDisplayName());
                if (!n.trim().isEmpty()) itemName = n.trim();
            }
        }
        // 订单数据只在详情页（两个订单按钮都在）解析；其他 Bazaar 页面仅显示操作栏
        if (BazzarInventoryMatcher.isItemDetailPage(cs)) {
            if (cfg.apiEnabled) {
                var info = fetchApiInfo(cs, center);
                if (info != null) {
                    // Hypixel 命名反直觉：buy_summary 是卖家报价，sell_summary 是买家订单。
                    // "购买订单"组显示买单(sellSummary)，"出售报价"组显示卖单(buySummary)
                    buys = toEntries(info.sellSummary(), TopOrderData.OrderType.BUY, cfg.maxLines);
                    sells = toEntries(info.buySummary(), TopOrderData.OrderType.SELL, cfg.maxLines);
                    if (itemName.isEmpty()) itemName = info.displayName();
                }
            }
            if (buys.isEmpty() && sells.isEmpty()) {
                TopOrderData.ParsedBazzarGui data = BazzarInventoryMatcher.parse(cs);
                if (data.valid()) {
                    buys = limit(data.buyOrders(), cfg.maxLines);
                    sells = limit(data.sellOrders(), cfg.maxLines);
                    if (itemName.isEmpty()) itemName = data.itemName();
                }
            }
        }
        if (!itemName.isEmpty()) {
            // flip 的 /bz 命令需要纯文本名：去色码 + 剥 "[SELL/BUY xxx]" 包装与整名括号（终极附魔书）
            lastParsedItemName = cleanCommandName(itemName);
        }
        final String plainName = ChatUtils.stripColor(itemName);

        String onText = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.flipOn");
        String offText = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.flipOff");
        String title = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.title");
        String subBuy = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.subBuy");
        String subSell = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.subSell");
        String editGui = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.editGui");
        String flipTipMain = "config.babyzombieaddons.overlay.bazzar.tooltip.flipMain";
        String flipTipBuy = "config.babyzombieaddons.overlay.bazzar.tooltip.flipBuy";
        String flipTipSell = "config.babyzombieaddons.overlay.bazzar.tooltip.flipSell";

        Font font = Minecraft.getInstance().font;
        int lineH = font.lineHeight + 2;

        // ===== Buy / Sell 组（详情页 + 订单页悬浮共用同一渲染） =====
        if (cfg.showBuyOrders) {
            buyTexts = buildOrderLines(buys,
                    "config.babyzombieaddons.overlay.bazzar.text.buyOrders",
                    "config.babyzombieaddons.overlay.bazzar.text.buyLineRest",
                    itemName, plainName, font);
        } else {
            buyTexts = List.of();
        }
        if (cfg.showSellOffers) {
            sellTexts = buildOrderLines(sells,
                    "config.babyzombieaddons.overlay.bazzar.text.sellOffers",
                    "config.babyzombieaddons.overlay.bazzar.text.sellLineRest",
                    itemName, plainName, font);
        } else {
            sellTexts = List.of();
        }

        // ===== Action Texts =====
        List<ClickableText> at = new ArrayList<>();
        int curY = 0;
        if (cfg.showActionBar) {
            // 操作栏自身开关（第一行，关闭后从设置页恢复）
            String showActionBarLine = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.showActionBar")
                    + (cfg.showActionBar ? onText : offText);
            at.add(new ClickableText(0, curY, "§7" + showActionBarLine, 0xFFFFFFFF,
                    List.of("config.babyzombieaddons.overlay.bazzar.tooltip.showActionBar"),
                    () -> { playClickSound(); toggleShowActionBar(); }));
            curY += lineH;
            at.add(new ClickableText(0, curY, editGui, 0xFFFFFFFF,
                    List.of("config.babyzombieaddons.overlay.bazzar.tooltip.editGui"),
                    () -> {
                        playClickSound();
                        HudManager.openEditScreen(Minecraft.getInstance().gui.screen(), HudTag.BAZAAR);
                    }));
            curY += lineH;
            String mainLine = "§f" + title + (cfg.flipEnabled ? onText : offText);
            at.add(new ClickableText(0, curY, mainLine, 0xFFFFFFFF,
                    List.of(flipTipMain), () -> { playClickSound(); toggleFlipMain(); }));
            curY += lineH;
            at.add(new ClickableText(6, curY, "§7" + subBuy + (cfg.flipBuyEnabled ? onText : offText),
                    0xFFFFFFFF, List.of(flipTipBuy), () -> { playClickSound(); toggleFlipBuy(); }));
            curY += lineH;
            at.add(new ClickableText(6, curY, "§7" + subSell + (cfg.flipSellEnabled ? onText : offText),
                    0xFFFFFFFF, List.of(flipTipSell), () -> { playClickSound(); toggleFlipSell(); }));
            curY += lineH;
            String showOrdersPageLine = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.showOrdersPage")
                    + (cfg.ordersPageEnabled ? onText : offText);
            at.add(new ClickableText(6, curY, "§7" + showOrdersPageLine, 0xFFFFFFFF,
                    List.of("config.babyzombieaddons.overlay.bazzar.tooltip.showOrdersPage"),
                    () -> { playClickSound(); toggleOrdersPageEnabled(); }));
            curY += lineH;
            String showBuyLine = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.showBuy")
                    + (cfg.showBuyOrders ? onText : offText);
            at.add(new ClickableText(6, curY, "§7" + showBuyLine, 0xFFFFFFFF,
                    List.of("config.babyzombieaddons.overlay.bazzar.tooltip.showBuy"),
                    () -> { playClickSound(); toggleShowBuyOrders(); }));
            curY += lineH;
            String showSellLine = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.showSell")
                    + (cfg.showSellOffers ? onText : offText);
            at.add(new ClickableText(6, curY, "§7" + showSellLine, 0xFFFFFFFF,
                    List.of("config.babyzombieaddons.overlay.bazzar.tooltip.showSell"),
                    () -> { playClickSound(); toggleShowSellOffers(); }));
            curY += lineH;
            String apiModeLine = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.apiMode")
                    + (cfg.apiEnabled ? onText : offText);
            at.add(new ClickableText(6, curY, "§7" + apiModeLine, 0xFFFFFFFF,
                    List.of("config.babyzombieaddons.overlay.bazzar.tooltip.apiMode"),
                    () -> { playClickSound(); toggleApiEnabled(); }));
            curY += lineH;
            String lineCountLine = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.lineCount", cfg.maxLines);
            at.add(new ClickableText(6, curY, "§7" + lineCountLine, 0xFFFFFFFF,
                    List.of("config.babyzombieaddons.overlay.bazzar.tooltip.lineCount"),
                    () -> { playClickSound(); openConfigSearch(); }));
            curY += lineH;
            String buyHistoryLine = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.buyHistory")
                    + (cfg.buyOrderHistoryEnabled ? onText : offText);
            at.add(new ClickableText(0, curY, "§f" + buyHistoryLine, 0xFFFFFFFF,
                    List.of("config.babyzombieaddons.overlay.bazzar.tooltip.buyHistory"),
                    () -> { playClickSound(); toggleBuyOrderHistory(); }));
            curY += lineH;
            String buyHistoryLines = ChatUtils.translate(
                    "config.babyzombieaddons.overlay.bazzar.text.buyHistoryLineCount", cfg.buyOrderHistoryMaxLines);
            at.add(new ClickableText(6, curY, "§7" + buyHistoryLines, 0xFFFFFFFF,
                    List.of("config.babyzombieaddons.overlay.bazzar.tooltip.buyHistoryLineCount"),
                    () -> { playClickSound(); openBuyHistoryLineCountSearch(); }));
            curY += lineH;
            // 数据时间戳（API 模式）：展示数据新鲜度，点击复制时间
            if (cfg.apiEnabled) {
                String timeText = formatSnapshotTime();
                String dataTimeLine = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.dataTime", timeText);
                at.add(new ClickableText(6, curY, "§7" + dataTimeLine, 0xFFFFFFFF,
                        List.of("config.babyzombieaddons.overlay.bazzar.tooltip.copyTime"),
                        () -> copyTimeAndToast(timeText)));
            }
        }
        actionTexts = at;
    }

    private static void saveCfg() { ModConfigManager.save(); }

    // ========== API 数据辅助 ==========

    /** 优先中间物品槽的 ItemStack（NBT id 精确映射），拿不到再用 title 显示名（含颜色码） */
    private static BazaarItemInfo.Info fetchApiInfo(AbstractContainerScreen<?> cs, ItemStack center) {
        if (center != null) {
            BazaarItemInfo.Info info = BazaarItemInfo.get(center);
            if (info != null) return info;
        }
        String rawName = BazzarInventoryMatcher.getRawItemName(cs);
        if (rawName != null) return BazaarItemInfo.get(rawName);
        return null;
    }

    /** 价格格式化：千分位 + 一位小数（与 GUI 解析/HUD 行格式一致） */
    private static String formatPrice(double v) {
        return String.format(Locale.ROOT, "%,.1f", v);
    }

    /** API 聚合桶 → 现有 TopOrderEntry 行格式（价格千分位一位小数，与 GUI 解析一致） */
    private static List<TopOrderData.TopOrderEntry> toEntries(
            List<BazaarItemInfo.SummaryTier> tiers, TopOrderData.OrderType type, int max) {
        List<TopOrderData.TopOrderEntry> out = new ArrayList<>();
        for (BazaarItemInfo.SummaryTier t : tiers) {
            if (out.size() >= max) break;
            String price = formatPrice(t.pricePerUnit());
            out.add(new TopOrderData.TopOrderEntry(price + " coins", price, (int) t.amount(), t.orders(), type));
        }
        return out;
    }

    /** GUI 解析数据截断到 max 行 */
    private static List<TopOrderData.TopOrderEntry> limit(
            List<TopOrderData.TopOrderEntry> list, int max) {
        return list.size() > max ? list.subList(0, max) : list;
    }

    // ========== 订单页：悬浮订单信息（仅 API） ==========

    /** 是否为 Bazaar 订单页（容器标题精确匹配；订单列表页无法从 GUI 解析订单数据） */
    private static boolean isOrdersPage(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?>)) return false;
        String title = ChatUtils.stripColor(screen.getTitle().getString()).trim();
        return "Bazaar Orders".equals(title) || "Co-op Bazaar Orders".equals(title);
    }

    /**
     * 定位鼠标下的物品槽（仅容器槽，跳过玩家背包；坐标算法与原版 hover 判定一致）。
     * Bazaar 订单页是原版箱子页（ContainerScreen, 176 宽, 114 + 行数*18 高，居中）：
     * leftPos/topPos 无法跨包读取，按同一公式计算。
     */
    private static Slot findHoveredSlot(AbstractContainerScreen<?> cs, int mx, int my) {
        if (!(cs instanceof ContainerScreen container)) return null;
        ChestMenu menu = container.getMenu();
        int rows = menu.getRowCount();
        var rect = container.getRectangle();
        int left = (rect.width() - 176) / 2;
        int top = (rect.height() - (114 + rows * 18)) / 2;
        var player = Minecraft.getInstance().player;
        for (Slot slot : menu.slots) {
            if (!slot.isActive() || !slot.hasItem()) continue;
            if (player != null && slot.container == player.getInventory()) continue;
            if (mx >= left + slot.x && mx < left + slot.x + 16
                    && my >= top + slot.y && my < top + slot.y + 16) {
                return slot;
            }
        }
        return null;
    }

    /**
     * 订单页物品 → API 数据。
     * 优先 NBT skyblock id（普通物品，附魔书含 enchantments 标签时也走 NBT 路径）；
     * 拿不到 id（订单页附魔书、属性碎片等）剥掉 "[SELL xxx]" / "[BUY xxx]" 包装按显示名识别。
     */
    private static BazaarItemInfo.Info fetchOrdersItemInfo(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        BazaarItemInfo.Info info = BazaarItemInfo.get(stack);
        if (info != null) return info;
        String name = unwrapOrdersDisplayName(stack.getDisplayName());
        if (name == null || name.isEmpty()) return null;
        return BazaarItemInfo.get(name);
    }

    /** 订单页显示名（保留原色） "[§6§lSELL §aVampiric Vitality V§b]" → "§aVampiric Vitality V§b"；无包装原样返回 */
    private static String unwrapOrdersDisplayNameColored(Component displayName) {
        String legacy = ChatUtils.toLegacyString(displayName).trim();
        int open = legacy.indexOf('[');
        int close = legacy.lastIndexOf(']');
        if (open >= 0 && close > open) {
            String inner = legacy.substring(open + 1, close).trim();
            int sp = inner.indexOf(' ');
            if (sp > 0) {
                String tag = ChatUtils.stripColor(inner.substring(0, sp)).trim().toUpperCase(Locale.ROOT);
                if ("SELL".equals(tag) || "BUY".equals(tag)) {
                    String c = inner.substring(sp + 1).trim();
                    if (!c.isEmpty()) return c;
                }
            }
        }
        return legacy;
    }

    /** 订单页显示名 "[SELL xxx]"（SELL/BUY 忽略大小写）→ "xxx"；无包装原样返回（已 trim） */
    private static String unwrapOrdersDisplayName(Component displayName) {
        String stripped = ChatUtils.stripColor(ChatUtils.toLegacyString(displayName));
        return unwrapBracketWrapper(stripped);
    }

    /** "[SELL xxx]" / "[BUY xxx]"（SELL/BUY 忽略大小写）→ "xxx"；无包装原样返回（已 trim） */
    private static String unwrapBracketWrapper(String text) {
        if (text == null) return null;
        String t = text.trim();
        int open = t.indexOf('[');
        int close = t.lastIndexOf(']');
        if (open >= 0 && close > open) {
            String core = t.substring(open + 1, close).trim();
            int sp = core.indexOf(' ');
            if (sp > 0) {
                String tag = core.substring(0, sp).toUpperCase(Locale.ROOT);
                if ("SELL".equals(tag) || "BUY".equals(tag)) {
                    String inner = core.substring(sp + 1).trim();
                    if (!inner.isEmpty()) return inner;
                }
            }
        }
        return t;
    }

    /**
     * 命令用干净物品名：剥 "[SELL/BUY xxx]" 包装，再剥整名成对括号
     * （终极附魔书显示名 "[Crop Fever V]"）。无括号时原样返回纯文本名。
     */
    private static String cleanCommandName(String itemName) {
        String plain = ChatUtils.stripColor(itemName);
        String s = unwrapBracketWrapper(plain);
        if (s.length() >= 2 && s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']') {
            String inner = s.substring(1, s.length() - 1).trim();
            if (!inner.isEmpty()) s = inner;
        }
        return s;
    }

    /**
     * 订单页悬浮：指到容器物品时用 API 数据填充 Buy/Sell 两个订单 HUD 列表
     * （与详情页同一渲染/同一 HUD 位置）。开关未开、API 未开、数据未就绪或
     * 未悬浮时列表保持为空；productId+快照时间命中时跳过重建。
     */
    private static void refreshOrdersPageLists(AbstractContainerScreen<?> cs, int mx, int my) {
        var cfg = getCfg();
        if (cfg == null || !cfg.ordersPageEnabled || !cfg.apiEnabled) {
            buyTexts = List.of(); sellTexts = List.of(); lastOrdersHoverKey = "";
            return;
        }
        boolean showBuy = cfg.showBuyOrders;
        boolean showSell = cfg.showSellOffers;
        if (!showBuy && !showSell) {
            buyTexts = List.of(); sellTexts = List.of(); lastOrdersHoverKey = "";
            return;
        }
        Slot slot = findHoveredSlot(cs, mx, my);
        ItemStack stack = (slot != null) ? slot.getItem() : null;
        if (stack == null || stack.isEmpty()) {
            buyTexts = List.of(); sellTexts = List.of(); lastOrdersHoverKey = "";
            return;
        }
        BazaarItemInfo.Info info = fetchOrdersItemInfo(stack);
        if (info == null) {
            buyTexts = List.of(); sellTexts = List.of(); lastOrdersHoverKey = "";
            return;
        }
        String key = info.productId();
        long ts = BazaarItemInfo.getSnapshotTs();
        if (key.equals(lastOrdersHoverKey) && ts == lastOrdersHoverTs
                && (!buyTexts.isEmpty() || !sellTexts.isEmpty())) {
            return;
        }
        // 标题物品名：保留原来的颜色码（从订单按钮的 ItemStack DisplayName 提取），不再沿用标题的 §6
        String coloredName = unwrapOrdersDisplayNameColored(stack.getDisplayName());
        String plainName = unwrapOrdersDisplayName(stack.getDisplayName());
        String headerItem = (coloredName == null || coloredName.isEmpty())
                ? info.displayName()
                : coloredName;
        String commandName = (plainName == null || plainName.isEmpty())
                ? info.displayName()
                : plainName;
        Font font = Minecraft.getInstance().font;
        int max = Math.max(1, cfg.maxLines);
        buyTexts = showBuy ? buildOrderLines(toEntries(info.sellSummary(), TopOrderData.OrderType.BUY, max),
                "config.babyzombieaddons.overlay.bazzar.text.buyOrders",
                "config.babyzombieaddons.overlay.bazzar.text.buyLineRest",
                headerItem, commandName, font) : List.of();
        sellTexts = showSell ? buildOrderLines(toEntries(info.buySummary(), TopOrderData.OrderType.SELL, max),
                "config.babyzombieaddons.overlay.bazzar.text.sellOffers",
                "config.babyzombieaddons.overlay.bazzar.text.sellLineRest",
                headerItem, commandName, font) : List.of();
        lastOrdersHoverKey = key;
        lastOrdersHoverTs = ts;
    }

    /**
     * 订单 HUD 行列表：标题行（可点击复制物品名）+ 价格列（可点击复制纯数字）。
     * 详情页与订单页悬浮共用。
     */
    private static List<ClickableText> buildOrderLines(List<TopOrderData.TopOrderEntry> entries,
                                                       String headerKey, String restKey,
                                                       String itemName, String plainName, Font font) {
        List<ClickableText> out = new ArrayList<>();
        if (entries.isEmpty()) return out;
        int lineH = font.lineHeight + 2;
        int curY = 0;
        out.add(new ClickableText(0, curY, ChatUtils.translate(headerKey, itemName), 0xFFFFFFFF,
                List.of("config.babyzombieaddons.overlay.bazzar.tooltip.copyName"),
                () -> copyNameAndToast(plainName)));
        curY += lineH;
        int idx = 1;
        for (var e : entries) {
            String prefix = "§7" + idx + ". ";
            String price = "§6" + e.priceRaw();
            String rest = ChatUtils.translate(restKey, e.amount(), e.orderCount());
            int pw = font.width(ChatUtils.stripColor(prefix));
            out.add(new ClickableText(0, curY, prefix + price + rest, 0xFFFFFFFF, List.of(), null));
            final String priceNumOnly = e.priceNumberOnly();
            out.add(new ClickableText(pw, curY, price, 0x00FFFFFF,
                    List.of("config.babyzombieaddons.overlay.bazzar.tooltip.copyPrice"),
                    () -> copyPriceAndToast(priceNumOnly)));
            idx++;
            curY += lineH;
        }
        return out;
    }

    /** 播放 MC 自带按钮点击音效（UI_BUTTON_CLICK） */
    private static void playClickSound() {
        try {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.0f);
            }
        } catch (Exception ignored) {}
    }

    /** 播放经验球音效，作为价格点击复制的反馈 */
    private static void playPriceClickSound() {
        try {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.3f, 1.0f);
            }
        } catch (Exception ignored) {}
    }

    private static void toggleFlipMain() {
        var cfg = getCfg(); if (cfg == null) return;
        cfg.flipEnabled = !cfg.flipEnabled; saveCfg(); rebuildTexts();
    }
    private static void toggleFlipBuy() {
        var cfg = getCfg(); if (cfg == null) return;
        cfg.flipBuyEnabled = !cfg.flipBuyEnabled; saveCfg(); rebuildTexts();
    }
    private static void toggleFlipSell() {
        var cfg = getCfg(); if (cfg == null) return;
        cfg.flipSellEnabled = !cfg.flipSellEnabled; saveCfg(); rebuildTexts();
    }

    private static void toggleShowBuyOrders() {
        var cfg = getCfg(); if (cfg == null) return;
        cfg.showBuyOrders = !cfg.showBuyOrders; saveCfg(); rebuildTexts();
    }

    private static void toggleShowSellOffers() {
        var cfg = getCfg(); if (cfg == null) return;
        cfg.showSellOffers = !cfg.showSellOffers; saveCfg(); rebuildTexts();
    }

    private static void toggleShowActionBar() {
        var cfg = getCfg(); if (cfg == null) return;
        cfg.showActionBar = !cfg.showActionBar; saveCfg(); rebuildTexts();
    }

    private static void toggleApiEnabled() {
        var cfg = getCfg(); if (cfg == null) return;
        cfg.apiEnabled = !cfg.apiEnabled; saveCfg(); rebuildTexts();
    }

    private static void toggleOrdersPageEnabled() {
        var cfg = getCfg(); if (cfg == null) return;
        cfg.ordersPageEnabled = !cfg.ordersPageEnabled; saveCfg(); rebuildTexts();
    }

    private static void toggleBuyOrderHistory() {
        var cfg = getCfg(); if (cfg == null) return;
        cfg.buyOrderHistoryEnabled = !cfg.buyOrderHistoryEnabled; saveCfg(); rebuildTexts();
    }

    /** 打开 Mod 设置页并搜索"行数"（定位 maxLines 配置） */
    private static void openConfigSearch() {
        Minecraft.getInstance().gui.setScreen(ModConfigManager.createGUI(Minecraft.getInstance().gui.screen(), Component.translatable("config.babyzombieaddons.option.bazzarMaxLines").getString()));
    }

    /** 打开 Mod 设置页并搜索"求购历史最大显示行数" */
    private static void openBuyHistoryLineCountSearch() {
        Minecraft.getInstance().gui.setScreen(ModConfigManager.createGUI(Minecraft.getInstance().gui.screen(),
                Component.translatable("config.babyzombieaddons.option.bazzarBuyOrderHistoryMaxLines").getString()));
    }

    private static void copyPriceAndToast(String priceNumberOnly) {
        ChatUtils.copyToClipboard(priceNumberOnly);
        ChatUtils.showToast("config.babyzombieaddons.overlay.bazzar.toast.copiedTitle",
                "config.babyzombieaddons.overlay.bazzar.toast.copiedBody", priceNumberOnly);
        playPriceClickSound();
    }

    /** 复制物品名（纯文本，无颜色码） */
    private static void copyNameAndToast(String name) {
        ChatUtils.copyToClipboard(name);
        ChatUtils.showToast("config.babyzombieaddons.overlay.bazzar.toast.copiedTitle",
                "config.babyzombieaddons.overlay.bazzar.toast.copiedBody", name);
        playPriceClickSound();
    }

    /** 快照时间戳 → "HH:mm:ss"（本地时区） */
    private static String formatSnapshotTime() {
        long ts = BazaarItemInfo.getSnapshotTs();
        if (ts <= 0) return "--:--";
        return java.time.Instant.ofEpochMilli(ts)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /** 复制数据时间 */
    private static void copyTimeAndToast(String timeText) {
        ChatUtils.copyToClipboard(timeText);
        ChatUtils.showToast("config.babyzombieaddons.overlay.bazzar.toast.copiedTimeTitle",
                "config.babyzombieaddons.overlay.bazzar.toast.copiedTimeBody", timeText);
        playPriceClickSound();
    }
}
