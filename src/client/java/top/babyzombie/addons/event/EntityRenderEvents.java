package top.babyzombie.addons.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;

/**
 * Custom event fired before an entity is rendered.
 * Cancellable: return true to cancel the render.
 */
public final class EntityRenderEvents {

    public static final Event<BeforeRender> BEFORE_RENDER =
            EventFactory.createArrayBacked(BeforeRender.class, callbacks -> (entity) -> {
                for (BeforeRender cb : callbacks) {
                    if (cb.beforeRender(entity)) return true;
                }
                return false;
            });

    @FunctionalInterface
    public interface BeforeRender {
        /** Return true to cancel rendering this entity. */
        boolean beforeRender(Entity entity);
    }
}
