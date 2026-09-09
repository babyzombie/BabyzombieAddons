package top.babyzombie.addons.module.chat.containerchat;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.config.ModConfig.ChatItemDisplayMode;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.render.ChatItemIconSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 接收端:聊天物品信息展示。
 * <p>匹配本模组发送格式的 [物品] 消息(方括号内以物品 ID 开头),按配置模式改写显示:
 * 灰色方括号、物品名按物品库颜色染色、可插入物品图标、
 * 括号内文本悬停显示物品信息;括号后的剩余文本恢复白色。
 * 只有能按 ID 从物品库拿到 ItemStack 的消息才会被改写,拿不到时保持原样不动颜色。
 */
public final class ChatItemDisplayModule {

    /** 物品 ID 匹配:大写字母/数字/下划线(可带 ;等级 后缀),后接可选的名字部分 */
    private static final Pattern ITEM_MESSAGE = Pattern.compile("([A-Z0-9_]+(?:;\\d+)?)(?:\\s+(.*))?");

    private ChatItemDisplayModule() {}

    public static void init() {
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            if (overlay) return message;
            var chat = ModConfigManager.get().general.chat;
            if (!chat.chatItemDisplay) return message;
            Component rewritten = rewrite(message, chat.chatItemDisplayMode);
            return rewritten != null ? rewritten : message;
        });
    }

    /**
     * 扫描消息里所有"能按 ID 从物品库解析出物品"的方括号 [ID 名]。
     * 格式像 ID 但解析不到物品的括号(如 [GM]/[MVP+] 等频道标签)一律跳过、
     * 保持原样不动颜色;无任何匹配返回空列表。
     */
    private static List<Match> locateAll(Component message) {
        String text = message.getString();
        List<Match> out = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '[') continue;
            int close = text.indexOf(']', i + 1);
            if (close < 0) break;
            String content = text.substring(i + 1, close).trim();
            Matcher m = ITEM_MESSAGE.matcher(content);
            if (!m.matches()) {
                i = close; // 非物品格式的括号,跳过继续找
                continue;
            }
            String id = m.group(1);
            ItemStack stack = ItemUtils.getItemStackByNeuName(id);
            if (stack == null || stack.isEmpty()) {
                i = close; // 解析不到物品,不算物品消息,跳过继续找
                continue;
            }
            out.add(new Match(i, close + 1, id, m.group(2), stack));
            i = close;
        }
        return out;
    }

    @Nullable
    private static Component rewrite(Component message, ChatItemDisplayMode mode) {
        List<Match> matches = locateAll(message);
        if (matches.isEmpty()) return null;

        // ModernUI 环境下含图标的文本由 GuiTextRenderStateMixin 回退到 vanilla 管线，
        // 图标渲染链路与纯 vanilla 一致，因此这里不再按 mod 抑制图标
        boolean showIcon = mode == ChatItemDisplayMode.ICON || mode == ChatItemDisplayMode.ICON_NAME;
        boolean showName = mode != ChatItemDisplayMode.ICON;

        String text = message.getString();
        MutableComponent result = Component.empty();
        int cursor = 0;
        for (Match match : matches) {
            appendSliced(result, message.toFlatList(), cursor, match.start(), false);
            result.append(buildBracket(match, showIcon, showName));
            cursor = match.end();
        }
        // 最后一个物品括号之后的剩余文本恢复白色
        appendSliced(result, message.toFlatList(), cursor, text.length(), true);
        return result;
    }

    /**
     * tooltip 第一行 = 物品显示名组件（所见即所得，含 CUSTOM_NAME / display.Name 颜色）。
     *
     * @return 第一行组件；栈为空或 tooltip 异常时 null
     */
    @Nullable
    private static Component firstTooltipLine(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        try {
            var lines = stack.getTooltipLines(
                    net.minecraft.world.item.Item.TooltipContext.EMPTY,
                    null,
                    net.minecraft.world.item.TooltipFlag.NORMAL);
            return lines.isEmpty() ? null : lines.get(0);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 单个物品括号:[ icon name ] */
    private static Component buildBracket(Match match, boolean showIcon, boolean showName) {
        ItemStack stack = match.stack();

        // 名字:消息带名字用消息的;否则用物品库的名字
        String shownName = match.name();
        // tooltip 第一行就是物品显示名组件(所见即所得,含 CUSTOM_NAME / display.Name 颜色)
        Component hoverName = firstTooltipLine(stack);
        String legacyName = hoverName != null
                ? ChatUtils.toLegacyString(hoverName)
                : ItemUtils.displayNameLegacy(stack);
        if (shownName == null) shownName = ChatUtils.stripColor(legacyName);
        // 颜色优先 tooltip 名组件的 style 颜色;再 item-repo 文件 displayname 兜底
        TextColor nameColor = hoverName != null ? hoverName.getStyle().getColor() : null;
        if (nameColor == null) nameColor = firstColor(legacyName);
        if (nameColor == null) {
            String repoName = ItemUtils.repoDisplayName(match.id());
            if (repoName != null) nameColor = firstColor(repoName);
        }

        MutableComponent result = Component.empty();
        result.append(Component.literal("[ ").withStyle(ChatFormatting.GRAY));
        if (showIcon) {
            result.append(Component.literal(String.valueOf(ChatItemIconSupport.MARKER))
                    .withStyle(s -> s.withHoverEvent(itemHover(stack))));
            result.append(Component.literal(" ").withStyle(ChatFormatting.GRAY));
        }
        if (showName) {
            Style nameStyle = Style.EMPTY;
            if (nameColor != null) nameStyle = nameStyle.withColor(nameColor);
            nameStyle = nameStyle.withHoverEvent(itemHover(stack));
            result.append(Component.literal(shownName).withStyle(nameStyle));
        }
        result.append(Component.literal(" ]").withStyle(ChatFormatting.GRAY));
        return result;
    }

    /** 按全局字符区间裁剪叶子组件追加到 out;forceWhite 时强制白色 */
    private static void appendSliced(MutableComponent out, List<Component> leaves, int from, int to, boolean forceWhite) {
        int cursor = 0;
        for (Component leaf : leaves) {
            String s = leaf.getString();
            int leafStart = cursor;
            cursor += s.length();
            if (cursor <= from || leafStart >= to) continue;
            int a = Math.max(leafStart, from) - leafStart;
            int b = Math.min(cursor, to) - leafStart;
            if (a >= b) continue;
            Style style = forceWhite
                    ? leaf.getStyle().withColor(TextColor.fromLegacyFormat(ChatFormatting.WHITE))
                    : leaf.getStyle();
            out.append(Component.literal(s.substring(a, b)).withStyle(style));
        }
    }

    private static HoverEvent itemHover(ItemStack stack) {
        // 与 fromNonEmptyStack 等效;不换后者以避免空栈抛异常,空栈由调用方防御
        return new HoverEvent.ShowItem(new ItemStackTemplate(stack.typeHolder(), stack.getCount(), stack.getComponentsPatch()));
    }

    /** 取 legacy 文本里第一个颜色码(§c 或 §x 六位 hex),无则 null */
    @Nullable
    private static TextColor firstColor(String legacy) {
        for (int i = 0; i + 1 < legacy.length(); i++) {
            if (legacy.charAt(i) != '§') continue;
            char code = legacy.charAt(i + 1);
            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                ChatFormatting fmt = ChatFormatting.getByCode(code);
                if (fmt != null) return TextColor.fromLegacyFormat(fmt);
                i++;
            } else if (code == 'x' || code == 'X') {
                if (i + 13 >= legacy.length()) return null;
                int rgb = 0;
                for (int k = 0; k < 6; k++) {
                    int digit = Character.digit(legacy.charAt(i + 3 + k * 2), 16);
                    if (digit < 0) return null;
                    rgb = (rgb << 4) | digit;
                }
                return TextColor.fromRgb(rgb);
            } else {
                i++;
            }
        }
        return null;
    }

    private record Match(int start, int end, String id, String name, ItemStack stack) {}
}
