package top.babyzombie.addons.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;

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

    public static final Event<BeforeCreate> BEFORE_CREATE =
            EventFactory.createArrayBacked(BeforeCreate.class, callbacks -> (ParticleOptions options, double x, double y, double z, double xa, double ya, double za) -> {
                for (BeforeCreate cb : callbacks) {
                    if (cb.beforeCreate(options, x, y, z, xa, ya, za)) return true;
                }
                return false;
            });

    @FunctionalInterface
    public interface BeforeCreate {
        boolean beforeCreate(ParticleOptions options, double x, double y, double z, double xa, double ya, double za);
    }
}
