package top.babyzombie.addons.module.misc.bazaar;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.sounds.SoundEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.hud.HudTag;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.gui.overlay.*;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.ArrayList;
import java.util.List;

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
            if (!BazzarInventoryMatcher.isBazzarScreen(s)) return;
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
        TopOrderData.ParsedBazzarGui data = (cs != null)
                ? BazzarInventoryMatcher.parse(cs)
                : TopOrderData.ParsedBazzarGui.EMPTY;

        if (data.itemName() != null && !data.itemName().isEmpty()) {
            lastParsedItemName = data.itemName();
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
        bt.add(new ClickableText(0, curY, ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.buyOrders", data.itemName()), 0xFFFFFFFF, List.of(), null));
        curY += lineH;
        int idx = 1;
        for (var e : data.buyOrders()) {
            String prefix = "§7" + idx + ". ";
            String price = "§6" + e.priceRaw();
            String rest = ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.buyLineRest", e.amount(), e.orderCount());
            int pw = font.width(ChatUtils.stripColor(prefix));
            bt.add(new ClickableText(0, curY, prefix + price + rest, 0xFFFFFFFF, List.of(), null));
            int pRelX = pw;
            final String priceNumOnly = e.priceNumberOnly();
            bt.add(new ClickableText(pRelX, curY, price, 0x00FFFFFF, List.of(copyTipKey),
                    () -> copyPriceAndToast(priceNumOnly)));
            idx++;
            curY += lineH;
        }
        buyTexts = bt;

        // ===== Sell Texts =====
        List<ClickableText> st = new ArrayList<>();
        curY = 0;
        st.add(new ClickableText(0, curY, ChatUtils.translate("config.babyzombieaddons.overlay.bazzar.text.sellOffers", data.itemName()), 0xFFFFFFFF, List.of(), null));
        curY += lineH;
        idx = 1;
        for (var e : data.sellOrders()) {
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
        sellTexts = st;

        // ===== Action Texts =====
        List<ClickableText> at = new ArrayList<>();
        curY = 0;
        at.add(new ClickableText(0, curY, editGui, 0xFFFFFFFF,
                List.of("config.babyzombieaddons.overlay.bazzar.tooltip.editGui"),
                () -> {
                    playClickSound();
                    HudManager.activeTag = HudTag.BAZAAR;
                    HudManager.save();
                    HudManager.openEditScreen(Minecraft.getInstance().gui.screen());
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
        actionTexts = at;
    }

    private static void saveCfg() { ModConfigManager.save(); }

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

    private static void copyPriceAndToast(String priceNumberOnly) {
        ChatUtils.copyToClipboard(priceNumberOnly);
        ChatUtils.showToast("config.babyzombieaddons.overlay.bazzar.toast.copiedTitle",
                "config.babyzombieaddons.overlay.bazzar.toast.copiedBody", priceNumberOnly);
        playPriceClickSound();
    }
}
