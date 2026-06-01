package top.babyzombie.addons.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.resources.sounds.SoundInstance;

/**
 * Custom event fired before a sound is played.
 * Cancellable: return true to cancel the sound.
 */
public final class PlaySoundEvents {

    public static final Event<BeforePlay> BEFORE_PLAY =
            EventFactory.createArrayBacked(BeforePlay.class, callbacks -> sound -> {
                for (BeforePlay cb : callbacks) {
                    if (cb.beforePlay(sound)) return true;
                }
                return false;
            });

    @FunctionalInterface
    public interface BeforePlay {
        boolean beforePlay(SoundInstance sound);
    }
}
