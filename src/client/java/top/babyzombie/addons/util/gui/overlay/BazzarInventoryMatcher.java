package top.babyzombie.addons.util.gui.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.babyzombie.addons.util.ChatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BazzarInventoryMatcher {
    private BazzarInventoryMatcher() {}

    private static final String BUY_NAME = "Create Buy Order";
    private static final String SELL_NAME = "Create Sell Offer";
    private static final String BUY_HEADER = "Top Orders:";
    private static final String SELL_HEADER = "Top Offers:";

    // 兼容 Hypixel 订单行格式（- 12.3 coins each | 250,504x in 4 orders）
    private static final Pattern ORDER_LINE =
            Pattern.compile("^-?\\s*([\\d,]+(?:\\.\\d+)?)\\s*coins\\s*(?:each)?\\s*\\|\\s*([\\d,]+)x?\\s*(?:in|from)\\s*(\\d+)\\s*(?:orders?|offers?)\\.?$");

    public static boolean isBazzarScreen(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?>)) return false;
        String title = ChatUtils.stripColor(screen.getTitle().getString());
        boolean arrow = title.contains("➜");
        var pair = findButtons((AbstractContainerScreen<?>) screen);
        boolean hasBoth = pair.a != null && pair.b != null;
        if (arrow) return true;
        return hasBoth;
    }

    public static TopOrderData.ParsedBazzarGui parse(AbstractContainerScreen<?> cs) {
        if (cs == null) return TopOrderData.ParsedBazzarGui.EMPTY;
        var pair = findButtons(cs);
        if (pair.a == null || pair.b == null) {
            return TopOrderData.ParsedBazzarGui.EMPTY;
        }
        List<String> buyLore = getTooltipLines(pair.a);
        List<String> sellLore = getTooltipLines(pair.b);
        // 物品名以 GUI title 的 "➜" 符号之后的文本为准，找不到时 fallback 到 Lore
        String screenTitle = ChatUtils.stripColor(cs.getTitle().getString());
        int arrowIdx = screenTitle.indexOf('➜');
        String itemName = (arrowIdx >= 0) ? screenTitle.substring(arrowIdx + 1).trim() : "";
        if (itemName.isEmpty()) itemName = findItemName(buyLore, BUY_HEADER);
        if (itemName.isEmpty()) itemName = findItemName(sellLore, SELL_HEADER);
        List<TopOrderData.TopOrderEntry> buys = extract(buyLore, BUY_HEADER, TopOrderData.OrderType.BUY);
        List<TopOrderData.TopOrderEntry> sells = extract(sellLore, SELL_HEADER, TopOrderData.OrderType.SELL);
        boolean valid = !buys.isEmpty() && !sells.isEmpty();
        return new TopOrderData.ParsedBazzarGui(itemName, buys, sells, valid);
    }

    /** 从界面 title 提取当前物品原始显示名（保留颜色码，供 API 门面识别终极附魔等） */
    public static String getRawItemName(AbstractContainerScreen<?> cs) {
        if (cs == null) return null;
        String raw = ChatUtils.toLegacyString(cs.getTitle());
        int arrowIdx = raw.indexOf('➜');
        if (arrowIdx < 0) return null;
        String name = raw.substring(arrowIdx + 1).trim();
        return name.isEmpty() ? null : name;
    }

    /**
     * 定位 Bazaar 详情页（三排容器）正中间的物品槽位：第 2 排第 5 号（index 13）。
     * 该槽位即当前查看物品，可直接读取 NBT 走精确映射。
     */
    public static ItemStack getCenterItem(AbstractContainerScreen<?> cs) {
        if (cs == null) return null;
        try {
            for (var slot : cs.getMenu().slots) {
                if (slot.container == null) continue;
                if (slot.container == Minecraft.getInstance().player.getInventory()) continue;
                if (slot.index != 13) continue;
                if (!slot.hasItem()) return null;
                return slot.getItem();
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ===================== helpers =====================

    private static class Pair { ItemStack a; ItemStack b; Pair(ItemStack x, ItemStack y) { a = x; b = y; } }

    private static Pair findButtons(AbstractContainerScreen<?> cs) {
        ItemStack buy = null, sell = null;
        try {
            // 定位玩家背包起始 slot，避免把玩家背包里的物品名误判成按钮
            int maxContainerIdx = -1;
            for (var slot : cs.getMenu().slots) {
                if (slot.container == null) continue;
                if (slot.container == Minecraft.getInstance().player.getInventory()) continue;
                if (slot.index > maxContainerIdx) maxContainerIdx = slot.index;
            }
            for (var slot : cs.getMenu().slots) {
                if (slot.container == Minecraft.getInstance().player.getInventory()) continue;
                if (slot.container == null) continue;
                if (!slot.hasItem()) continue;
                if (maxContainerIdx >= 0 && slot.index > maxContainerIdx) continue;
                ItemStack is = slot.getItem();
                String name = ChatUtils.stripColor(is.getDisplayName().getString());
                if (name == null) continue;
                // 宽松匹配：包含关键子串即可（避免尾部空格/控制字符造成精确匹配失败）
                String n = name.trim();
                boolean isBuy = n.equalsIgnoreCase(BUY_NAME)
                        || (n.contains("Buy") && n.contains("Order") && !n.contains("Top"));
                boolean isSell = n.equalsIgnoreCase(SELL_NAME)
                        || (n.contains("Sell") && (n.contains("Offer") || n.contains("Sell Order")) && !n.contains("Top"));
                if (isBuy && buy == null) {
                    buy = is;
                }
                else if (isSell && sell == null) {
                    sell = is;
                }
                if (buy != null && sell != null) break;
            }
        } catch (Exception ignored) {}
        return new Pair(buy, sell);
    }

    /**
     * 统一使用 ItemStack.getTooltipLines() 获取完整 Lore（包括 DisplayName 后的
     * 所有行、带 Component 合并的 extra 文本），再 stripColor 成纯文本匹配。
     */
    private static List<String> getTooltipLines(ItemStack stack) {
        List<String> out = new ArrayList<>();
        if (stack == null) return out;
        try {
            var mc = Minecraft.getInstance();
            var ctx = mc.level != null
                    ? net.minecraft.world.item.Item.TooltipContext.of(mc.level)
                    : net.minecraft.world.item.Item.TooltipContext.EMPTY;
            List<Component> lines = stack.getTooltipLines(ctx, mc.player, TooltipFlag.Default.NORMAL);
            for (var c : lines) {
                String s = ChatUtils.stripColor(ChatUtils.removeEmoji(ChatUtils.toLegacyString(c)));
                if (s != null) out.add(s.trim());
            }
        } catch (Exception ignored) {}
        return out;
    }

    /** 在 header 之前的行里寻找首个非空的物品名 */
    private static String findItemName(List<String> lore, String header) {
        for (String line : lore) {
            if (line == null) continue;
            if (line.contains(header)) break;
            if (line.isEmpty()) continue;
            if (BUY_NAME.equals(line) || SELL_NAME.equals(line)) continue;
            String cleaned = line.trim();
            if (!cleaned.isEmpty()) return cleaned;
        }
        return "";
    }

    private static List<TopOrderData.TopOrderEntry> extract(List<String> lore, String header, TopOrderData.OrderType type) {
        List<TopOrderData.TopOrderEntry> out = new ArrayList<>();
        boolean pastHeader = false;
        for (String line : lore) {
            if (!pastHeader) {
                if (line != null && line.contains(header)) {
                    pastHeader = true;
                }
                continue;
            }
            if (line == null || line.isEmpty()) continue;
            // 避免后续 Click/Inventory 行被误判
            if (line.startsWith("Click") || line.startsWith("Right-Click") || line.startsWith("Inventory:")) {
                break;
            }
            Matcher m = ORDER_LINE.matcher(line);
            if (!m.matches()) continue;
            String priceRaw = m.group(1);
            int amount; int orders;
            try {
                amount = Integer.parseInt(m.group(2).replace(",", ""));
                orders = Integer.parseInt(m.group(3).replace(",", ""));
            } catch (Exception e) {
                continue;
            }
            out.add(new TopOrderData.TopOrderEntry(priceRaw + " coins", priceRaw, amount, orders, type));
        }
        return out;
    }
}
