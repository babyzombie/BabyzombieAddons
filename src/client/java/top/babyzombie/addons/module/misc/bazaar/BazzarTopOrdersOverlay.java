package top.babyzombie.addons.module.misc.bazaar;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
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

    private BazzarTopOrdersOverlay() {}

    public static void init() {
        GuiOverlayManager.register(INSTANCE);

        // Tick 节流重建（无缓存承诺：每次都完整重新 parse）
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            var cfg = getCfg();
            if (cfg == null || !cfg.overlayEnabled) return;
            Screen s = Minecraft.getInstance().gui.screen();
            // Bazaar 界面（含列表页）就刷新数据，操作栏常显；订单数据仅详情页解析
            if (!BazzarInventoryMatcher.isBazzarScreen(s)) return;
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
            if (text == null) return;
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
        return BazzarInventoryMatcher.isBazzarScreen(screen);
    }

    @Override public void onInventoryUpdated() { rebuildTexts(); }

    @Override
    public void render(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (!(Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> cs)) return;
        Font font = Minecraft.getInstance().font;
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
        // 真实物品名：优先中间槽 ItemStack 显示名（游戏内权威名），数据源无关
        ItemStack center = BazzarInventoryMatcher.getCenterItem(cs);
        String itemName = "";
        if (center != null) {
            // 显示用名保留原色（如 §d§lCrop Fever V）
            String n = ChatUtils.toLegacyString(center.getDisplayName());
            if (!n.trim().isEmpty()) itemName = n.trim();
        }
        if (cs != null) {
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
            // flip 的 /bz 命令需要纯文本名，不能带颜色码
            lastParsedItemName = ChatUtils.stripColor(itemName);
        }

        String copyTipKey = "config.babyzombieaddons.overlay.bazzar.tooltip.copyPrice";
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

        // ===== Buy Texts =====
        List<ClickableText> bt = new ArrayList<>();
        int curY = 0;
        if (cfg.showBuyOrders && !buys.isEmpty()) {
            bt.add(new ClickableText(0, curY, ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.buyOrders", itemName), 0xFFFFFFFF, List.of(), null));
            curY += lineH;
            int idx = 1;
            for (var e : buys) {
                String prefix = "§7" + idx + ". ";
                String price = "§6" + e.priceRaw();
                String rest = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.buyLineRest", e.amount(), e.orderCount());
                int pw = font.width(ChatUtils.stripColor(prefix));
                bt.add(new ClickableText(0, curY, prefix + price + rest, 0xFFFFFFFF, List.of(), null));
                final String priceNumOnly = e.priceNumberOnly();
                bt.add(new ClickableText(pw, curY, price, 0x00FFFFFF, List.of(copyTipKey),
                        () -> copyPriceAndToast(priceNumOnly)));
                idx++;
                curY += lineH;
            }
        }
        buyTexts = bt;

        // ===== Sell Texts =====
        List<ClickableText> st = new ArrayList<>();
        curY = 0;
        if (cfg.showSellOffers && !sells.isEmpty()) {
            st.add(new ClickableText(0, curY, ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.sellOffers", itemName), 0xFFFFFFFF, List.of(), null));
            curY += lineH;
            int idx = 1;
            for (var e : sells) {
                String prefix = "§7" + idx + ". ";
                String price = "§6" + e.priceRaw();
                String rest = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.sellLineRest", e.amount(), e.orderCount());
                int pw = font.width(ChatUtils.stripColor(prefix));
                st.add(new ClickableText(0, curY, prefix + price + rest, 0xFFFFFFFF, List.of(), null));
                final String priceNumOnly = e.priceNumberOnly();
                st.add(new ClickableText(pw, curY, price, 0x00FFFFFF, List.of(copyTipKey),
                        () -> copyPriceAndToast(priceNumOnly)));
                idx++;
                curY += lineH;
            }
        }
        sellTexts = st;

        // ===== Action Texts =====
        List<ClickableText> at = new ArrayList<>();
        curY = 0;
        if (cfg.showActionBar) {
            // 操作栏自身开关（第一行，关闭后从设置页恢复）
            String showActionBarLine = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.showActionBar")
                    + (cfg.showActionBar ? onText : offText);
            at.add(new ClickableText(0, curY, "§7" + showActionBarLine, 0xFFFFFFFF, List.of(),
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
            String showBuyLine = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.showBuy")
                    + (cfg.showBuyOrders ? onText : offText);
            at.add(new ClickableText(6, curY, "§7" + showBuyLine, 0xFFFFFFFF, List.of(),
                    () -> { playClickSound(); toggleShowBuyOrders(); }));
            curY += lineH;
            String showSellLine = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.showSell")
                    + (cfg.showSellOffers ? onText : offText);
            at.add(new ClickableText(6, curY, "§7" + showSellLine, 0xFFFFFFFF, List.of(),
                    () -> { playClickSound(); toggleShowSellOffers(); }));
            curY += lineH;
            String apiModeLine = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.apiMode")
                    + (cfg.apiEnabled ? onText : offText);
            at.add(new ClickableText(6, curY, "§7" + apiModeLine, 0xFFFFFFFF, List.of(),
                    () -> { playClickSound(); toggleApiEnabled(); }));
            curY += lineH;
            String lineCountLine = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.lineCount", cfg.maxLines);
            at.add(new ClickableText(6, curY, "§7" + lineCountLine, 0xFFFFFFFF, List.of(),
                    () -> { playClickSound(); openConfigSearch(); }));
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

    /** API 聚合桶 → 现有 TopOrderEntry 行格式（价格千分位一位小数，与 GUI 解析一致） */
    private static List<TopOrderData.TopOrderEntry> toEntries(
            List<BazaarItemInfo.SummaryTier> tiers, TopOrderData.OrderType type, int max) {
        List<TopOrderData.TopOrderEntry> out = new ArrayList<>();
        for (BazaarItemInfo.SummaryTier t : tiers) {
            if (out.size() >= max) break;
            String price = String.format(Locale.ROOT, "%,.1f", t.pricePerUnit());
            out.add(new TopOrderData.TopOrderEntry(price + " coins", price, (int) t.amount(), t.orders(), type));
        }
        return out;
    }

    /** GUI 解析数据截断到 max 行 */
    private static List<TopOrderData.TopOrderEntry> limit(
            List<TopOrderData.TopOrderEntry> list, int max) {
        return list.size() > max ? list.subList(0, max) : list;
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

    /** 打开 Mod 设置页并搜索"行数"（定位 maxLines 配置） */
    private static void openConfigSearch() {
        Minecraft.getInstance().gui.setScreen(ModConfigManager.createGUI(Minecraft.getInstance().gui.screen(), Component.translatable("config.babyzombieaddons.option.bazzarMaxLines").getString()));
    }

    private static void copyPriceAndToast(String priceNumberOnly) {
        ChatUtils.copyToClipboard(priceNumberOnly);
        ChatUtils.showToast("config.babyzombieaddons.overlay.bazzar.toast.copiedTitle",
                "config.babyzombieaddons.overlay.bazzar.toast.copiedBody", priceNumberOnly);
        playPriceClickSound();
    }
}
