package top.babyzombie.addons.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.particle.Particle;

public final class ParticleRenderEvents {

    public static final Event<BeforeAdd> BEFORE_ADD =
            EventFactory.createArrayBacked(BeforeAdd.class, callbacks -> (particle) -> {
                for (BeforeAdd cb : callbacks) {
                    if (cb.beforeAdd(particle)) return true;
                }
                return false;
            });

    @FunctionalInterface
    public interface BeforeAdd {
        /** Return true to cancel adding this particle. */
        boolean beforeAdd(Particle particle);
    }
}
