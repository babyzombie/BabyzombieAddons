package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

public final class PowderMiningSounds {
    private static long blockBreakTimer;

    private PowderMiningSounds() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().mining.powderMiningSounds) return;
            if (!isInCrystalHollows()) return;
            if (client.player != null && client.player.swinging) {
                blockBreakTimer = ServerTick.getTime() + 5000;
            }
        });

        PlaySoundEvents.BEFORE_PLAY.register(sound -> {
            if (!ModConfigManager.get().mining.powderMiningSounds) return false;
            if (!isInCrystalHollows()) return false;
            if (blockBreakTimer < ServerTick.getTime()) return false;

            String snd = sound.getSound().toString();
            if (snd.contains("random.orb")) {
                return true; // cancel XP orb sounds
            }
            return false;
        });
    }

    private static boolean isInCrystalHollows() {
        var t = HypixelLocationTracker.getInstance();
        return t.isInSkyblock() && "Crystal Hollows".equals(t.getMap());
    }
}
