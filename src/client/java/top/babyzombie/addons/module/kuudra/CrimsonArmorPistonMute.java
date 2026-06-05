package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.HypixelLocationTracker;

/**
 * Mutes piston sound when Crimson Isle armor is at max stacks.
 */
public final class CrimsonArmorPistonMute {
    private static boolean muted;

    private CrimsonArmorPistonMute() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!ModConfigManager.get().kuudra.muteCrimsonArmor) return;
            if (!overlay) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

            String text = message.getString();
            muted = text.contains("10⁑") || text.contains("10ᝐ") || text.contains("10Ѫ");
        });

        PlaySoundEvents.BEFORE_PLAY.register(sound -> {
            if (!muted) return false;
            if (!ModConfigManager.get().kuudra.muteCrimsonArmor) return false;
            var snd = sound.getSound();
            if (snd == null) return false;
            String path = snd.getLocation().getPath();
            return path.contains("tile/piston/in") || path.contains("tile/piston/out");
        });
    }
}
