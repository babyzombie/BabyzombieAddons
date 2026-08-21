package top.babyzombie.addons.module.chat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import me.shedaniel.rei.api.client.gui.screen.DisplayScreen;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.entry.EntryStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * REI 集成辅助类。所有 REI 相关的类型引用都在 {@link ReiBridge} 中隔离，
 * 确保 REI 未安装时不会触发 {@link NoClassDefFoundError}。
 * 配方详情页内部槽位（impl 包 EntryWidget）不直接引用类型，用反射调用，
 * 内部结构变化时静默降级而非崩溃。
 */
public final class ReiHelper {

    private static final boolean REI_LOADED = FabricLoader.getInstance().isModLoaded("roughlyenoughitems");

    private ReiHelper() {}

    public static boolean isLoaded() {
        return REI_LOADED;
    }

    /**
     * 获取 REI 中当前鼠标悬停的物品的显示名称，优先级：
     * <ol>
     *   <li>DisplayScreen 配方详情页内部槽位（impl EntryWidget 反射调用）</li>
     *   <li>overlay 物品列表的 focused stack</li>
     * </ol>
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
            if (screen instanceof DisplayScreen) return true;
            String name = screen.getClass().getName().toLowerCase();
            return name.contains("roughlyenoughitems") || (name.contains(".rei.") && name.contains("screen"));
        }

        static String getHoveredEntryName() {
            try {
                // 1) 配方详情页内部槽位（EntryWidget 是 impl 包类，不编译期引用，
                //    反射调 getCurrentEntry；方法/类变化时抛普通异常被兜住，静默降级）
                var screen = Minecraft.getInstance().gui.screen();
                if (screen instanceof DisplayScreen) {
                    String name = displayScreenHoveredName(screen);
                    if (name != null) return name;
                }

                // 2) overlay 物品列表
                var overlay = me.shedaniel.rei.api.client.REIRuntime.getInstance().getOverlay();
                if (overlay.isEmpty()) return null;
                var stack = overlay.get().getEntryList().getFocusedStack();
                if (stack.isEmpty()) return null;
                return entryName(stack);
            } catch (Exception | LinkageError e) {
                // Exception: 反射缺失/不可访问等，REI 内部结构变化时静默降级
                // LinkageError: 类结构整体跳变（NoClassDefFoundError 等是 Error 不是 Exception）
                return null;
            }
        }

        private static String displayScreenHoveredName(Screen screen) {
            // 26.1 的 MouseHandler.xpos/ypos 返回物理像素坐标，REI widget bounds 是 GUI 缩放坐标，
            // 必须除以 guiScale 才能命中 containsMouse
            double scale = Minecraft.getInstance().getWindow().getGuiScale();
            double mx = Minecraft.getInstance().mouseHandler.xpos() / scale;
            double my = Minecraft.getInstance().mouseHandler.ypos() / scale;
            java.util.IdentityHashMap<Widget, Boolean> visited = new java.util.IdentityHashMap<>();
            for (GuiEventListener child : screen.children()) {
                if (child instanceof Widget w) {
                    String name = findHovered(w, mx, my, visited);
                    if (name != null) return name;
                }
            }
            return null;
        }

        /**
         * 在 widget 子树中查找鼠标悬停的槽位。
         * 不用 REI 的 Widgets.walk：REI 所有 widget 都实现 vanilla ContainerEventHandler
         * 但 children() 返回空，真正的子级（setupDisplay 槽位列表）藏在复合 widget 的
         * widgets 字段里，walk 的 else-if 分支永远到不了。
         */
        private static String findHovered(Widget widget, double mx, double my,
                                          java.util.IdentityHashMap<Widget, Boolean> visited) {
            if (visited.put(widget, Boolean.TRUE) != null) return null;

            // 1) 复合 widget 的内部列表（DisplayCompositeWidget.widgets = setupDisplay 槽位列表）
            try {
                var field = widget.getClass().getDeclaredField("widgets");
                field.setAccessible(true);
                Object value = field.get(widget);
                if (value instanceof List<?> list) {
                    for (Object o : list) {
                        if (o instanceof Widget w) {
                            String name = findHovered(w, mx, my, visited);
                            if (name != null) return name;
                        }
                    }
                }
            } catch (ReflectiveOperationException ignored) {
                // 非复合 widget，无内部列表
            }

            // 2) 容器 children 递归（PaddedWidget 等包装结构）
            for (GuiEventListener child : widget.children()) {
                if (child instanceof Widget w) {
                    String name = findHovered(w, mx, my, visited);
                    if (name != null) return name;
                }
            }

            // 3) 自身是槽位（EntryWidget 有 getCurrentEntry）
            if (widget.containsMouse(mx, my)) {
                try {
                    var method = widget.getClass().getMethod("getCurrentEntry");
                    Object entry = method.invoke(widget);
                    if (entry instanceof EntryStack<?> es && !es.isEmpty()) {
                        return entryName(es);
                    }
                } catch (ReflectiveOperationException ignored) {
                    // 非槽位 widget（面板/按钮/装饰）
                }
            }
            return null;
        }

        private static String entryName(EntryStack<?> stack) {
            Object value = stack.getValue();
            if (value instanceof ItemStack itemStack) {
                String name = itemStack.getHoverName().getString();
                if (itemStack.getCount() > 1) {
                    name += " x" + itemStack.getCount();
                }
                return name;
            }
            return stack.asFormattedText().getString();
        }
    }
}
