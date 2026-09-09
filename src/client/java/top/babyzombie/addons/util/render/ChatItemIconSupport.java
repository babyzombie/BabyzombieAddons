package top.babyzombie.addons.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * 聊天物品图标辅助：
 * <ul>
 *   <li>{@link #MARKER}：一个通用标记码点（私用区），占位"在这里画一个物品"；</li>
 *   <li>物品本体通过 style 的 {@link HoverEvent.ShowItem} 携带（完整 ItemStack，含 NBT）。</li>
 * </ul>
 */
public final class ChatItemIconSupport {

    /**
     * 通用标记码点：所有物品共用，区分靠 style 里的 ItemStack。
     *
     * <p>选号说明：早期用 \uE000 被运行时字体（SkyHanni/Skyblocker 状态条一类）占用，
     * 导致裸标记字符显示成对方字形。\uF200 处于私用区 E000–F8FF，经查证
     * vanilla 默认字体、Hypixel 服务器资源包、本机用户资源包、48 个 mod 内置字体
     * 均不覆盖该段；若未来仍与其他运行时字体冲突，改这一个常量即可。
     */
    public static final char MARKER = '\uF200';

    /** item atlas 的纹理 id（缓存，避免使用已弃用的 TextureAtlas.LOCATION_ITEMS）。 */
    private static Identifier itemAtlasLocation;

    private ChatItemIconSupport() {}

    /**
     * 从 style 的 SHOW_ITEM hover 里取出 ItemStack。
     *
     * @return 有则返回 ItemStack，否则 null
     */
    public static ItemStack stackFromStyle(Style style) {
        HoverEvent hover = style.getHoverEvent();
        if (hover instanceof HoverEvent.ShowItem(ItemStackTemplate item)) {
            ItemStack stack = item.create();
            return stack.isEmpty() ? null : stack;
        }
        return null;
    }

    /** item atlas 的纹理 id（非弃用路径：AtlasManager → TextureAtlas.location()）。 */
    public static Identifier itemAtlasLocation() {
        if (itemAtlasLocation == null) {
            try {
                itemAtlasLocation = Minecraft.getInstance().getAtlasManager()
                        .getAtlasOrThrow(AtlasIds.ITEMS).location();
            } catch (Exception e) {
                itemAtlasLocation = Identifier.withDefaultNamespace("textures/atlas/items.png");
            }
        }
        return itemAtlasLocation;
    }
}