package top.babyzombie.addons.module.misc.auction;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.SkyblockConfig.AuctionQuickSell;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.mixin.screen.AbstractSignEditScreenAccessor;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AH 快速上架(仅告示牌填写,无鼠标移动)。
 * <p>
 * 移植自 ChatTriggers 版 misc.js 的 QuickAuction,裁剪为两部分:
 * <ul>
 *   <li>价格告示牌:点 "Item price: ..." 按钮后自动填入价格(上次上架价 → Skyblocker 价格 - 压价),只填不自动确认;</li>
 *   <li>时长告示牌:点 "Custom Duration" 按钮后固定填入 336(14 天上限)并自动完成。</li>
 * </ul>
 * 上次上架价缓存在点击创建页的 "Create BIN Auction" 提交按钮时从按钮 lore 更新,10 分钟有效。
 */
public final class AuctionQuickSellModule {

    /** 创建 BIN 上架页容器标题 */
    private static final String CONTAINER_CREATE = "Create BIN Auction";
    /** 自定义时长选择页容器标题 */
    private static final String CONTAINER_DURATION = "Auction Duration";

    /** "Create BIN Auction" 提交按钮名,其 lore 带 物品/时长/价格 三行 */
    private static final String SUBMIT_BUTTON_NAME = "Create BIN Auction";
    /** 时长选择页的"自定义时长"按钮名,点击后打开时长告示牌 */
    private static final String CUSTOM_DURATION_NAME = "Custom Duration";

    /** 创建页中待上架物品所在槽位(原版布局,服务端多年未变) */
    private static final int ITEM_SLOT = 13;
    /** 创建页中提交按钮所在槽位(4 行 3 列,布局不变),lore 的 "Item: xxx" 即物品名 */
    private static final int SUBMIT_SLOT = 29;

    /** 自定义时长固定填 14 天(336 小时,服务器上限) */
    private static final int MAX_DURATION_HOURS = 336;

    /** 上次上架价缓存有效时间(与原版一致) */
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;
    /** 点按钮到告示牌打开的待填窗口,过期作废避免串台 */
    private static final long PENDING_TTL_MS = 2000L;

    private static final Pattern LORE_ITEM = Pattern.compile("Item: (.+)");
    private static final Pattern LORE_PRICE = Pattern.compile("Item price: ([0-9,]+) coins");

    /** 物品名(去色)→ 上次上架价 */
    private static final Map<String, CachedPrice> priceCache = new HashMap<>();

    /** 等待被下一个告示牌消费的填充请求,只消费一次 */
    private static Pending pending;

    private AuctionQuickSellModule() {}

    public static void init() {
        ContainerClickEvents.BEFORE_MOUSE_CLICK.register(AuctionQuickSellModule::onContainerClick);
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> onScreenOpen(screen));
    }

    private static AuctionQuickSell getCfg() {
        return ModConfigManager.get().skyblock.auctionQuickSell;
    }

    // ========== 容器点击:识别价格/时长按钮与缓存写入 ==========

    private static boolean onContainerClick(AbstractContainerScreen<?> screen, Slot slot, MouseButtonEvent event) {
        if (!getCfg().auctionQuickSellEnabled) return false;
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return false;
        if (slot == null || !slot.hasItem()) return false;

        String title = ChatUtils.stripColor(screen.getTitle().getString()).trim();
        String name = ChatUtils.stripColor(slot.getItem().getHoverName().getString()).trim();

        switch (title) {
            case CONTAINER_CREATE -> {
                if (SUBMIT_BUTTON_NAME.equals(name)) {
                    // 点提交 = 本次上架完成:按钮 lore 带 Item:/Item price:,写入"上次上架价"缓存
                    cacheFromSubmitLore(slot.getItem());
                } else if (name.startsWith("Item price: ")) {
                    // 点价格按钮 → 下一个告示牌自动填价(只填不自动确认)
                    pending = new Pending(SignType.PRICE, computePriceText(screen.getMenu()));
                }
                // "Duration: ..." 按钮 → 打开时长选择页,无需处理
            }
            case CONTAINER_DURATION -> {
                if (CUSTOM_DURATION_NAME.equals(name)) {
                    // 自定义时长告示牌:固定填最大值并自动完成
                    pending = new Pending(SignType.DURATION, String.valueOf(MAX_DURATION_HOURS));
                }
            }
        }
        return false;
    }

    /** 从按钮 lore 解析 物品名/价格 写入缓存(与上架动作同步,失败静默) */
    private static void cacheFromSubmitLore(ItemStack submit) {
        String itemName = findLoreLine(submit, LORE_ITEM);
        String price = findLoreLine(submit, LORE_PRICE);
        if (itemName == null || price == null) return;
        try {
            long value = Long.parseLong(price.replace(",", ""));
            priceCache.put(itemName, new CachedPrice(value, System.currentTimeMillis()));
        } catch (NumberFormatException ignored) {}
    }

    /** 计算价格告示牌填充文本:缓存 → Skyblocker 价格 - 压价 → 无数据返回 null(手动输入) */
    private static String computePriceText(AbstractContainerMenu menu) {
        if (menu == null) return null;
        ItemStack item = slotItem(menu, ITEM_SLOT);
        if (item.isEmpty()) return null;

        String itemName = readSubmitItemName(menu);
        if (itemName == null) return null;

        CachedPrice cached = priceCache.get(itemName);
        if (cached != null && System.currentTimeMillis() - cached.ts < CACHE_TTL_MS) {
            return String.valueOf(cached.price);
        }

        double price = ItemUtils.getItemPrice(item);
        if (price <= 0) return null; // 无 Skyblocker 价格数据:本次不填,上架后缓存即生效
        long undercut = Math.max(1, getCfg().auctionQuickSellUndercut);
        return String.valueOf(Math.max(1, (long) price - undercut));
    }

    /** 提交按钮 lore 的 "Item: xxx" 作为物品名(与缓存写入同源,保证 key 一致) */
    private static String readSubmitItemName(AbstractContainerMenu menu) {
        ItemStack submit = slotItem(menu, SUBMIT_SLOT);
        if (submit.isEmpty()) return null;
        return findLoreLine(submit, LORE_ITEM);
    }

    // ========== 告示牌自动填充 ==========

    private static void onScreenOpen(net.minecraft.client.gui.screens.Screen screen) {
        if (!getCfg().auctionQuickSellEnabled) return;
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
        if (!(screen instanceof SignEditScreen signScreen)) {
            pending = null; // 非告示牌界面:作废待填状态(点按钮后应直接进入告示牌)
            return;
        }
        Pending p = pending;
        pending = null; // 只消费一次
        if (p == null || p.text == null || System.currentTimeMillis() - p.ts > PENDING_TTL_MS) return;
        // 告示牌行内容可能比屏幕打开晚到,延迟重试等待数据(与 Bazaar 贴入同节奏)
        Scheduler.schedule(0, new SignFillTask(signScreen, p));
    }

    /** 写入第一行(仅当首行为空,不覆盖玩家输入);返回 true 表示本次处理完成(填好或无需填) */
    private static boolean fill(SignEditScreen screen, Pending p) {
        String[] messages = ((AbstractSignEditScreenAccessor) screen).messages();
        if (messages == null || messages.length == 0 || messages[1].isEmpty()) return false; // 行内容未到,等待重试
        String firstLine = messages[0] == null ? "" : ChatUtils.stripColor(messages[0]).trim();
        if (!firstLine.isEmpty()) return true; // 玩家已输入,不覆盖
        messages[0] = p.text;
        return true;
    }

    /** 告示牌行内容晚到时,带最大尝试次数的重试填充任务 */
    private static final class SignFillTask implements Runnable {
        private static final int MAX_ATTEMPTS = 5;
        private final SignEditScreen screen;
        private final Pending pending;
        private int attempts;

        SignFillTask(SignEditScreen screen, Pending pending) {
            this.screen = screen;
            this.pending = pending;
        }

        @Override
        public void run() {
            if (Minecraft.getInstance().gui.screen() != screen) return; // 屏幕已关闭/切换,放弃
            if (fill(screen, pending)) {
                if (pending.type == SignType.DURATION)
                    Scheduler.schedule(1, screen::onClose); // 时长:填入即完成
                return;
            }
            if (++attempts < MAX_ATTEMPTS) Scheduler.schedule(3, this);
        }
    }

    // ========== 工具 ==========

    private static ItemStack slotItem(AbstractContainerMenu menu, int slotId) {
        if (slotId < 0 || slotId >= menu.slots.size()) return ItemStack.EMPTY;
        return menu.getSlot(slotId).getItem();
    }

    /** 读取物品 lore 中匹配的首行(去色后匹配,返回去掉前缀的捕获内容) */
    private static String findLoreLine(ItemStack stack, Pattern pattern) {
        var lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;
        for (var line : lore.lines()) {
            String s = ChatUtils.stripColor(ChatUtils.toLegacyString(line)).trim();
            Matcher m = pattern.matcher(s);
            if (m.matches()) return m.group(1).trim();
        }
        return null;
    }

    private enum SignType { PRICE, DURATION }

    /** 等待被告示牌消费的填充请求 */
    private static final class Pending {
        final SignType type;
        final String text;
        final long ts;

        Pending(SignType type, String text) {
            this.type = type;
            this.text = text;
            this.ts = System.currentTimeMillis();
        }
    }

    private static final class CachedPrice {
        final long price;
        final long ts;

        CachedPrice(long price, long ts) {
            this.price = price;
            this.ts = ts;
        }
    }
}