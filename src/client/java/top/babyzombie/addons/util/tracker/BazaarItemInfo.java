package top.babyzombie.addons.util.tracker;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.pet.RomanNumeral;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bazaar 物品信息门面：统一入口。
 *
 * <p>输入 ItemStack 或物品 id / 显示名 → 输出 API 完整数据（quick_status + 订单阶梯）。
 * 内部处理：物品 id 提取（NBT）、显示名规则转换、API 数据 60s 节流刷新。
 * 不传物品参数 → 只做 API 数据更新检测。
 *
 * <p>可靠路径是 ItemStack：NBT 物品 id 为完整形式（附魔书如 {@code ULTIMATE_CROP_FEVER;5}），
 * 经 {@link BazaarStockMapper} 精确映射。显示名路径仅尽力而为——ultimate/turbo/twilight 等
 * 前缀附魔的游戏内显示名（如 {@code [Crop Fever V]}）不带前缀，规则转换会失败，返回 null。
 */
public final class BazaarItemInfo {
    private BazaarItemInfo() {}

    /** 只做 API 数据更新检测（60s 节流）。数据就绪前查询返回 null，可稍后重查。 */
    public static void ensureFresh() {
        BazaarApiTracker.getInstance().ensureFresh();
    }

    /** 服务端快照时间（ms），-1 表示还没拉到过数据 */
    public static long getSnapshotTs() {
        return BazaarApiTracker.getInstance().getSnapshotTs();
    }

    /** ItemStack → 完整 bazaar 数据。NBT 物品 id 直查，最可靠；查不到返回 null。 */
    @Nullable
    public static Info get(ItemStack stack) {
        if (stack == null) return null;
        String skyblockId = ItemUtils.getSkyblockId(stack);
        // 附魔书：通用 id ENCHANTED_BOOK，实际附魔在 enchantments 标签（小写名:等级）
        if ("ENCHANTED_BOOK".equals(skyblockId)) {
            String ench = extractEnchantmentId(stack);
            if (ench != null) skyblockId = ench;
        }
        return get(skyblockId);
    }

    /**
     * 附魔书 NBT 适配：enchantments 标签（如 {"corruption": 1}）→ "CORRUPTION;1"，
     * 走 bazaarstocks 映射到 ENCHANTMENT_CORRUPTION_1。取第一个附魔（bazaar 附魔书为单附魔产品）。
     */
    @Nullable
    private static String extractEnchantmentId(ItemStack stack) {
        try {
            var customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) return null;
            var tag = customData.copyTag();
            var ench = tag.getCompound("enchantments").orElse(null);
            if (ench == null) return null;
            for (Map.Entry<String, net.minecraft.nbt.Tag> e : ench.entrySet()) {
                if (e.getValue() instanceof net.minecraft.nbt.NumericTag num) {
                    return e.getKey().toUpperCase(Locale.ROOT) + ";" + num.intValue();
                }
            }
        } catch (RuntimeException ignored) {}
        return null;
    }

    /**
     * 物品 id 或显示名 → 完整 bazaar 数据。
     * 先按物品 id 直查（普通物品 / 附魔书 {@code 名;等级} 格式），失败再走显示名规则转换。
     */
    @Nullable
    public static Info get(String itemIdOrName) {
        if (itemIdOrName == null || itemIdOrName.isBlank()) return null;
        ensureFresh();
        String raw = itemIdOrName.trim();
        String plain = ChatUtils.stripColor(raw).trim();

        // 1) 按物品 id 直查
        String productId = BazaarStockMapper.getInstance().lookupProductId(plain);
        Info info = productId != null ? build(productId, plain) : null;
        if (info != null) return info;

        // 2) 显示名规则转换兜底（普通显示名在直查阶段 build 会失败，走到这里）
        String converted = tryNameToId(raw);
        String convertedProduct = BazaarStockMapper.getInstance().lookupProductId(converted);
        if (convertedProduct == null) return null;
        return build(convertedProduct, plain);
    }

    private static Info build(String productId, String displayName) {
        BazaarApiTracker.ProductData data = BazaarApiTracker.getInstance().getProduct(productId);
        if (data == null) return null;
        return new Info(
                productId,
                displayName,
                data.buyPrice(), data.sellPrice(),
                data.buyVolume(), data.sellVolume(),
                data.buyOrders(), data.sellOrders(),
                data.buySummary(), data.sellSummary(),
                BazaarApiTracker.getInstance().getSnapshotTs());
    }

    // ===== 显示名 → 物品 id（尽力而为） =====
    // raw 保留颜色码：终极附魔游戏内加粗（§l），可直接识别
    // "Mist Shard" → "SHARD_MIST"（规则）；特例（18 个）走 NEU displayname 反查
    // "[Crop Fever V]"（加粗）→ "ULTIMATE_CROP_FEVER;5"；"Scavenger IV" → "SCAVENGER;4"
    // "Stock of Stonks" → "STOCK_OF_STONKS"
    // "Vampiric Vitality V"（游戏名 ≠ 内部名 MANA_VAMPIRE;5）→ 书页 lore 反查表兜底
    @Nullable
    private static String tryNameToId(String raw) {
        boolean bold = raw.contains("§l");
        String s = ChatUtils.stripColor(raw).trim();

        // 1) 属性碎片：显示名都以 " Shard" 结尾，"X Shard" → "SHARD_X" 规则零 I/O 命中；
        //    规则生成名不在 stock 集合的（内部代号 ≠ 怪物真名）走 NEU displayname 反查
        if (s.endsWith(" Shard")) {
            String core = s.substring(0, s.length() - " Shard".length());
            String rule = "SHARD_" + core.toUpperCase(Locale.ROOT).replace(' ', '_');
            if (BazaarStockMapper.getInstance().isKnownStock(rule)) return rule;
            return BazaarStockMapper.getInstance().lookupByDisplayName(s);
        }

        // 2) Essence（无 NBT id 的 head 类）："Safari Essence" → "ESSENCE_SAFARI"。
        //    essence 产品 id 即 API 产品键（不在 bazaarstocks 映射里），规则生成后由 build 验证存在性
        if (s.endsWith(" Essence")) {
            String core = s.substring(0, s.length() - " Essence".length());
            return "ESSENCE_" + core.toUpperCase(Locale.ROOT).replace(' ', '_');
        }

        // 3) 附魔书解析
        if (s.startsWith("Enchanted Book")) {
            s = s.substring("Enchanted Book".length()).trim();
        }
        if (s.startsWith("[") && s.endsWith("]")) {
            s = s.substring(1, s.length() - 1).trim();
        } else if (s.startsWith("(") && s.endsWith(")")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        // 末段罗马数字 = 附魔等级
        int sp = s.lastIndexOf(' ');
        if (sp > 0) {
            int lvl = RomanNumeral.parse(s.substring(sp + 1));
            if (lvl >= 1 && lvl <= 10) {
                String core = s.substring(0, sp).toUpperCase(Locale.ROOT).replace(' ', '_');
                // 加粗 = 终极附魔：优先拼 ULTIMATE_ 前缀（如 "[Crop Fever V]" 加粗）
                if (bold) {
                    String ult = "ULTIMATE_" + core + ";" + lvl;
                    if (BazaarStockMapper.getInstance().lookupProductId(ult) != null) return ult;
                }
                // 直接命中（普通附魔，显示名带完整前缀，如 "Scavenger IV" → SCAVENGER;4）
                String direct = core + ";" + lvl;
                if (BazaarStockMapper.getInstance().lookupProductId(direct) != null) return direct;
                // ultimate 系显示名缺前缀（如 "[Crop Fever V]" → ULTIMATE_CROP_FEVER;5）：
                // 核心名结尾匹配兜底，唯一命中才用
                String endMatch = BazaarStockMapper.getInstance().lookupEnchantByCore(core, lvl);
                if (endMatch != null) return endMatch;
                // 显示名与内部名不一致的附魔（如 "Vampiric Vitality V" → MANA_VAMPIRE;5）：
                // 书页 lore 反查表兜底（skyblocker 同款方案）
                String byDisplay = BazaarStockMapper.getInstance().lookupByEnchantDisplay(s);
                if (byDisplay != null) return byDisplay;
            }
        }
        // 4) 普通物品：大写 + 空格/连字符 → 下划线
        return s.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    /** 单档订单：同一价位的聚合（数量、单价、订单数） */
    public record SummaryTier(long amount, double pricePerUnit, int orders) {}

    /** 单个物品的完整 bazaar 信息。snapshotTs 为服务端快照时间，-1 表示数据未就绪。 */
    public record Info(
            String productId,
            String displayName,
            double buyPrice, double sellPrice,
            long buyVolume, long sellVolume,
            int buyOrders, int sellOrders,
            List<SummaryTier> buySummary,
            List<SummaryTier> sellSummary,
            long snapshotTs
    ) {
        /** @return 数据是否就绪（至少拉到过一次快照） */
        public boolean ready() { return snapshotTs > 0; }
    }
}
