package top.babyzombie.addons.module.minigames.ravengard;

import net.fabricmc.fabric.api.client.rendering.v1.ExtractItemDecorationsCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.regex.Pattern;

/**
 * Ravengard 小游戏（Hypixel 新的搜打撤小游戏）辅助功能。
 * 所有功能仅在 Ravengard 模式下生效：{@code RAVENGARD_HUB} / {@code RAVENGARD_DUNGEON_TRIO}。
 */
public final class RavengardModule {

    /**
     * 匹配 lore 中的售价行，例如 "§f👑§x§F§F§C§E§4§712§x§F§F§C§E§4§7 §x§F§F§C§E§4§7Crowns"。
     * 皇冠用 👑 转义（U+1F451），避免源文件编码导致的字符不一致；价格可能带千分位逗号。
     */
    private static final Pattern CROWNS_LINE = Pattern.compile("👑\\s*([\\d,]+)\\s*Crowns");

    private RavengardModule() {}

    /** 注册物品装饰渲染回调：在物品图标右下角绘制 Crowns 价格。 */
    public static void init() {
        ExtractItemDecorationsCallback.EVENT.register((graphics, font, stack, x, y) -> {
            if (!isPriceDisplayActive()) return;
            Long price = getCrownsPrice(stack);
            if (price == null) return;

            String s = String.valueOf(price);
            // 与原版堆叠数量相同的右下角位置：右对齐 + 阴影，价格用黄色渲染。
            // 注意：MC 颜色是 ARGB，alpha 在最高字节，缺 alpha 会被 text() 直接丢弃
            int px = x + 19 - 2 - font.width(s);
            int py = y + 6 + 3;
            // 堆叠数量 > 1 时原版会在右下角画数量，价格上移一行避免重叠
            if (stack.getCount() > 1) py = y;
            graphics.text(font, s, px, py, 0xFFFFFF55, true);
        });
    }

    /** 是否处于 Ravengard 小游戏（Hub 或 三人地牢）。 */
    public static boolean isInRavengard() {
        String mode = HypixelLocationTracker.getInstance().getMode();
        return mode != null && mode.startsWith("RAVENGARD_");
    }

    /** 价格显示功能是否开启且当前处于 Ravengard。 */
    public static boolean isPriceDisplayActive() {
        return ModConfigManager.get().minigames.ravengard.priceDisplay && isInRavengard();
    }

    /**
     * 从物品 lore 中解析 Crowns 售价。
     *
     * @return 单价；物品不可出售（lore 无售价行）时返回 null
     */
    public static Long getCrownsPrice(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;
        for (var line : lore.lines()) {
            String text = ChatUtils.stripColor(line.getString());
            var matcher = CROWNS_LINE.matcher(text);
            if (matcher.find()) {
                try {
                    return Long.parseLong(matcher.group(1).replace(",", ""));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
