package top.babyzombie.addons.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/**
 * Fired before a mouse click is processed in a container screen.
 * Return true to cancel the click.
 */
public final class ContainerClickEvents {

    public static final Event<BeforeMouseClick> BEFORE_MOUSE_CLICK =
            EventFactory.createArrayBacked(BeforeMouseClick.class, callbacks -> (screen, slot) -> {
                for (BeforeMouseClick cb : callbacks) {
                    if (cb.beforeMouseClick(screen, slot)) return true;
                }
                return false;
            });

    @FunctionalInterface
    public interface BeforeMouseClick {
        boolean beforeMouseClick(AbstractContainerScreen<?> screen, Slot slot);
    }
}
