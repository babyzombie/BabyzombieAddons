package top.babyzombie.addons.module.chat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * REI 集成辅助类。所有 REI 相关的类型引用都在 {@link ReiBridge} 中隔离，
 * 确保 REI 未安装时不会触发 {@link NoClassDefFoundError}。
 */
public final class ReiHelper {

    private static final boolean REI_LOADED = FabricLoader.getInstance().isModLoaded("roughlyenoughitems");

    private ReiHelper() {}

    public static boolean isLoaded() {
        return REI_LOADED;
    }

    /**
     * 获取 REI 物品列表/配方页面中当前鼠标悬停的物品的显示名称。
     *
     * @return 物品名称，如果没有悬停物品则返回 null
     */
    @Nullable
    public static String getHoveredEntryName() {
        if (!REI_LOADED) return null;
        return ReiBridge.getHoveredEntryName();
    }

    /**
     * 检查当前屏幕是否是 REI 的配方查看页面。
     * 通过 REI 的 DisplayScreen 接口判断，比字符串类名匹配可靠。
     */
    public static boolean isReiDisplayScreen(Object screen) {
        if (!REI_LOADED || screen == null) return false;
        return ReiBridge.isReiDisplayScreen(screen);
    }

    /**
     * 内部桥接类 —— 仅当 REI 已加载时才会被 JVM 加载，
     * 避免在 REI 未安装时因类型解析失败而崩溃。
     * 注意：仅放真正需要 REI 类型的方法，纯字符串操作放在 ReiHelper 本身。
     */
    private static final class ReiBridge {

        static boolean isReiDisplayScreen(Object screen) {
            if (screen instanceof me.shedaniel.rei.api.client.gui.screen.DisplayScreen) return true;
            String name = screen.getClass().getName().toLowerCase();
            return name.contains("roughlyenoughitems") || (name.contains(".rei.") && name.contains("screen"));
        }

        static String getHoveredEntryName() {
            try {
                var overlay = me.shedaniel.rei.api.client.REIRuntime.getInstance().getOverlay();
                if (overlay.isEmpty()) return null;
                var stack = overlay.get().getEntryList().getFocusedStack();
                if (stack.isEmpty()) return null;
                Object value = stack.getValue();
                if (value instanceof ItemStack itemStack) {
                    String name = itemStack.getHoverName().getString();
                    if (itemStack.getCount() > 1) {
                        name += " x" + itemStack.getCount();
                    }
                    return name;
                }
                return stack.asFormattedText().getString();
            } catch (Exception e) {
                return null;
            }
        }
    }
}
