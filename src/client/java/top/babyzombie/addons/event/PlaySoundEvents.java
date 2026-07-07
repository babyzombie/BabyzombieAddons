package top.babyzombie.addons.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.resources.sounds.SoundInstance;

/**
 * Custom events fired before a sound is played.
 */
public final class PlaySoundEvents {

    /** Fired before play. Return true to cancel the sound entirely. */
    public static final Event<BeforePlay> BEFORE_PLAY =
            EventFactory.createArrayBacked(BeforePlay.class, callbacks -> sound -> {
                for (BeforePlay cb : callbacks) {
                    if (cb.beforePlay(sound)) return true;
                }
                return false;
            });

    /**
     * Fired before play, allows modifying the SoundInstance.
     * Return a new SoundInstance (or wrap the original) to change volume/pitch/etc.
     * Return the original unchanged if no modifications are needed.
     */
    public static final Event<ModifySound> MODIFY =
            EventFactory.createArrayBacked(ModifySound.class, callbacks -> sound -> {
                SoundInstance current = sound;
                for (ModifySound cb : callbacks) {
                    current = cb.modify(current);
                }
                return current;
            });

    @FunctionalInterface
    public interface BeforePlay {
        /** @return true to cancel the sound */
        boolean beforePlay(SoundInstance sound);
    }

    @FunctionalInterface
    public interface ModifySound {
        /** @return the (possibly modified) SoundInstance to play */
        SoundInstance modify(SoundInstance original);
    }
}
