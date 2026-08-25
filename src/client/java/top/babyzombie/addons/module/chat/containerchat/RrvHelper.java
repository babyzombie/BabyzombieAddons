package top.babyzombie.addons.module.chat.containerchat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * RRV (Reliable Recipe Viewer) 集成辅助类。所有 RRV 相关的类型引用都在 {@link RrvBridge} 中隔离，
 * 确保 RRV 未安装时不会触发 {@link NoClassDefFoundError}。
 */
public final class RrvHelper {

    private static final boolean RRV_LOADED = FabricLoader.getInstance().isModLoaded("rrv");

    private RrvHelper() {}

    public static boolean isLoaded() {
        return RRV_LOADED;
    }

    /**
     * 获取 RRV 中当前鼠标悬停的物品的显示名称，优先级：
     * <ol>
     *   <li>RecipeViewScreen 配方详情页内部槽位（反射读私有字段，字段缺失时静默降级）</li>
     *   <li>物品列表 / 书签 overlay 的 hovered 槽位</li>
     * </ol>
     *
     * @return 物品名称，如果没有悬停物品则返回 null
     */
    @Nullable
    public static String getHoveredEntryName() {
        if (!RRV_LOADED) return null;
        return RrvBridge.getHoveredEntryName();
    }

    /**
     * 检查当前屏幕是否是 RRV 的配方查看页面（RecipeViewScreen）等非容器屏幕。
     * 通过 RecipeViewScreen 接口判断，类名匹配兜底。
     */
    public static boolean isRrvScreen(Object screen) {
        if (!RRV_LOADED || screen == null) return false;
        return RrvBridge.isRrvScreen(screen);
    }

    /**
     * 内部桥接类 —— 仅当 RRV 已加载时才会被 JVM 加载，
     * 避免在 RRV 未安装时因类型解析失败而崩溃。
     */
    private static final class RrvBridge {

        static boolean isRrvScreen(Object screen) {
            if (screen instanceof cc.cassian.rrv.common.recipe.inventory.RecipeViewScreen) return true;
            String name = screen.getClass().getName().toLowerCase();
            return name.contains("cc.cassian.rrv") || name.contains("reliable_recipe_viewer");
        }

        static String getHoveredEntryName() {
            try {
                // 1) 配方详情页内部槽位（hoveredSlot / workstationSlot 是私有字段，用反射读；
                //    字段名随 RRV 升级变化时抛普通异常，被外层 catch 兜住，静默降级不崩溃）
                var screen = Minecraft.getInstance().screen;
                if (screen instanceof cc.cassian.rrv.common.recipe.inventory.RecipeViewScreen rvs) {
                    String name = recipeViewHoveredName(rvs);
                    if (name != null) return name;
                }

                // 2) 物品列表 / 书签 overlay
                var itemView = cc.cassian.rrv.common.overlay.itemlist.view.ItemViewOverlay.INSTANCE;
                if (itemView.isEnabled()) {
                    String name = overlayHoveredName(itemView.itemSlots());
                    if (name != null) return name;
                }

                var sidePanel = cc.cassian.rrv.common.overlay.itemlist.panel.SidePanelOverlay.INSTANCE;
                if (sidePanel.isEnabled()) {
                    String name = overlayHoveredName(sidePanel.itemSlots());
                    if (name != null) return name;
                }
                return null;
            } catch (Exception | LinkageError e) {
                // Exception: 反射字段缺失/不可访问等，RRV 内部结构变化时静默降级
                // LinkageError: 类结构整体跳变（NoClassDefFoundError 等是 Error 不是 Exception）
                return null;
            }
        }

        private static String recipeViewHoveredName(cc.cassian.rrv.common.recipe.inventory.RecipeViewScreen screen)
                throws ReflectiveOperationException {
            // 配方输入/输出槽位（protected net.minecraft.world.inventory.Slot，RRV 每帧更新）
            var hoveredField = cc.cassian.rrv.common.recipe.inventory.RecipeViewScreen.class
                    .getDeclaredField("hoveredSlot");
            hoveredField.setAccessible(true);
            Object hovered = hoveredField.get(screen);
            if (hovered instanceof net.minecraft.world.inventory.Slot slot && slot.hasItem()) {
                return name(slot.getItem());
            }

            // 工作台槽位（private ItemSlot）
            var workstationField = cc.cassian.rrv.common.recipe.inventory.RecipeViewScreen.class
                    .getDeclaredField("workstationSlot");
            workstationField.setAccessible(true);
            Object workstation = workstationField.get(screen);
            if (workstation instanceof cc.cassian.rrv.common.overlay.ItemSlot itemSlot && itemSlot.isHovered()) {
                return name(itemSlot.getStack());
            }
            return null;
        }

        private static String overlayHoveredName(java.util.List<cc.cassian.rrv.common.overlay.ItemSlot> slots) {
            for (var slot : slots) {
                if (!slot.isHovered()) continue;
                return name(slot.getStack());
            }
            return null;
        }

        private static String name(ItemStack stack) {
            String name = stack.getHoverName().getString();
            if (stack.getCount() > 1) {
                name += " x" + stack.getCount();
            }
            return name;
        }
    }
}
