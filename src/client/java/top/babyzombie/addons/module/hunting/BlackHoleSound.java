package top.babyzombie.addons.module.hunting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.PlaySoundHelper;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * Adjusts the volume of the teleport sound repeatedly played by a nearby
 * Black Hole deployable while it pulls monsters.
 */
public final class BlackHoleSound {

    private static final String NAME_PREFIX = "Black Hole ";
    private static final double XZ_RANGE = 0.5;
    private static final double Y_RANGE = 3.0;

    private BlackHoleSound() {}

    public static void init() {
        PlaySoundEvents.MODIFY.register(BlackHoleSound::modifySound);
    }

    private static SoundInstance modifySound(SoundInstance sound) {
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return sound;
        var theSound = sound.getSound();
        if (theSound == null) return sound;
        if (!theSound.getLocation().getPath().startsWith("mob/endermen/portal")) return sound;

        var player = Minecraft.getInstance().player;
        if (player == null) return sound;

        if (isNearBlackHole(player.level(), sound)) {
            return PlaySoundHelper.withVolume(sound, ModConfigManager.get().hunting.blackHoleVolume);
        }
        return sound;
    }

    /** Whether a "Black Hole " armor stand is near the sound source. */
    private static boolean isNearBlackHole(Level level, SoundInstance sound) {
        var box = new AABB(
                sound.getX() - XZ_RANGE, sound.getY() - Y_RANGE, sound.getZ() - XZ_RANGE,
                sound.getX() + XZ_RANGE, sound.getY() + Y_RANGE, sound.getZ() + XZ_RANGE
        );
        for (var entity : level.getEntitiesOfClass(ArmorStand.class, box)) {
            var name = entity.getCustomName();
            if (name != null && name.getString().startsWith(NAME_PREFIX)) return true;
        }
        return false;
    }
}
