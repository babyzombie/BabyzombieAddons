package top.babyzombie.addons.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

/**
 * Container-related click events.
 * <ul>
 *   <li>{@link #BEFORE_MOUSE_CLICK} — 真实鼠标点击，在 screen 层触发，cancel 可阻止</li>
 *   <li>{@link #BEFORE_CONTAINER_INPUT} — 所有容器操作（含程序化点击）的最终公共路径，在发包前触发</li>
 * </ul>
 */
public final class ContainerClickEvents {

    // ---- Mouse click (screen layer, real player clicks only) ----

    public static final Event<BeforeMouseClick> BEFORE_MOUSE_CLICK =
            EventFactory.createArrayBacked(BeforeMouseClick.class, callbacks -> (screen, slot, event) -> {
                for (BeforeMouseClick cb : callbacks) {
                    if (cb.beforeMouseClick(screen, slot, event)) return true;
                }
                return false;
            });

    @FunctionalInterface
    public interface BeforeMouseClick {
        boolean beforeMouseClick(AbstractContainerScreen<?> screen, Slot slot, MouseButtonEvent event);
    }

    // ---- Container input (final common path, fires for all sources) ----

    public static final Event<BeforeContainerInput> BEFORE_CONTAINER_INPUT =
            EventFactory.createArrayBacked(BeforeContainerInput.class, callbacks -> (player, containerId, slotId, buttonNum, input) -> {
                for (BeforeContainerInput cb : callbacks) {
                    if (cb.beforeContainerInput(player, containerId, slotId, buttonNum, input)) return true;
                }
                return false;
            });

    @FunctionalInterface
    public interface BeforeContainerInput {
        /**
         * @param player      本地玩家
         * @param containerId 容器 ID
         * @param slotId      槽位索引
         * @param buttonNum   按键编号 (0=左键, 1=右键, …)
         * @param input       操作类型 (PICKUP, THROW, …)
         * @return true to cancel the input before the packet is sent
         */
        boolean beforeContainerInput(Player player, int containerId, int slotId, int buttonNum, ContainerInput input);
    }
}
