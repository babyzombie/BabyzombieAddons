package top.babyzombie.addons.module.garden;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.util.RandomSource;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

public final class XpOrbSoundReducer {
    private XpOrbSoundReducer() {}

    public static void init() {
        PlaySoundEvents.MODIFY.register(original -> {
            if (!shouldReduce(original)) return original;
            int volumePercent = ModConfigManager.get().garden.xpOrbSoundRemoval;
            if (volumePercent >= 100) return original;
            return wrap(original, original.getVolume() * (volumePercent / 100f));
        });
    }

    private static boolean shouldReduce(SoundInstance sound) {
        var theSound = sound.getSound();
        if (theSound == null) return false;
        if (!theSound.getLocation().getPath().equals("random/orb")) return false;
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return false;
        var location = HypixelLocationTracker.getInstance().getLocation();
        if (location == null || !location.equals("Garden")) return false;
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        return ItemUtils.isFarmingTool(player.getMainHandItem());
    }

    /** Mirror all fields from original, only overriding volume. */
    private static SoundInstance wrap(SoundInstance orig, float newVolume) {
        return new SimpleSoundInstance(
                orig.getIdentifier(), orig.getSource(), newVolume, orig.getPitch(),
                RandomSource.create(), orig.isLooping(), orig.getDelay(), orig.getAttenuation(),
                orig.getX(), orig.getY(), orig.getZ(), orig.isRelative()
        ) {
            @Override public float getVolume() { return newVolume; }
        };
    }
}
