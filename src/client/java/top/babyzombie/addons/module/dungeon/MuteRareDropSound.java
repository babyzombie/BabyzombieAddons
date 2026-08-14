package top.babyzombie.addons.module.dungeon;

import net.minecraft.sounds.SoundEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

public class MuteRareDropSound {
    private MuteRareDropSound() {}

    public static void init() {
        PlaySoundEvents.BEFORE_PLAY.register(sound -> {
            if (!ModConfigManager.get().dungeon.muteRareDropSound) return false;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return false;
            if (!HypixelLocationTracker.getInstance().isIn("Dungeon Hub")) return false;

            var identifier = sound.getIdentifier();
            float p = sound.getPitch();
            if (identifier.equals(SoundEvents.NOTE_BLOCK_PLING.value().location())) {
                return Math.abs(p - 0.59f) < 0.01f || Math.abs(p - 0.79f) < 0.01f
                        || Math.abs(p - 1.05f) < 0.01f || Math.abs(p - 1.17f) < 0.01f;
            }
            if (identifier.equals(SoundEvents.EXPERIENCE_ORB_PICKUP.location())) {
                return Math.abs(p - 0.70f) < 0.01f || Math.abs(p - 0.94f) < 0.01f
                        || Math.abs(p - 1.25f) < 0.01f || Math.abs(p - 1.41f) < 0.01f;
            }
            return false;
        });
    }
}
