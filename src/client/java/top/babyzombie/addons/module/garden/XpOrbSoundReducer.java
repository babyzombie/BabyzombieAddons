package top.babyzombie.addons.module.garden;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

public final class XpOrbSoundReducer {
    private XpOrbSoundReducer() {}

    public static void init() {
        // Cancel entirely when volume is 0
        PlaySoundEvents.BEFORE_PLAY.register(sound -> {
            if (ModConfigManager.get().garden.xpOrbSoundRemoval > 0) return false;
            return shouldReduce(sound);
        });
    }

    /**
     * Called from PlaySoundMixin @Redirect to adjust getVolume().
     * Returns proportional volume based on the 0-100 slider,
     * where 0 = fully muted, 100 = unchanged.
     */
    public static float getAdjustedVolume(SoundInstance sound, float originalVolume) {
        int volumePercent = ModConfigManager.get().garden.xpOrbSoundRemoval;
        if (volumePercent >= 100) return originalVolume;
        if (!shouldReduce(sound)) return originalVolume;
        if (volumePercent <= 0) return 0f;
        return originalVolume * (volumePercent / 100f);
    }

    private static boolean shouldReduce(SoundInstance sound) {
        var snd = sound.getSound();
        if (snd == null) return false;
        String path = snd.getLocation().getPath();
        if (!path.equals("random/orb"))
            return false;

        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return false;
        String location = HypixelLocationTracker.getInstance().getLocation();
        if (location == null || !location.equals("Garden")) return false;

        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        var held = player.getMainHandItem();
        return !held.isEmpty() && (held.getItem() instanceof HoeItem || held.getItem() instanceof AxeItem);
    }
}
