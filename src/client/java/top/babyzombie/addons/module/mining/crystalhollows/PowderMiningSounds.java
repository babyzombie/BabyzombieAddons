package top.babyzombie.addons.module.mining.crystalhollows;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

public final class PowderMiningSounds {
    private static long blockBreakTimer;

    private PowderMiningSounds() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().mining.crystalHollows.powderMiningSounds) return;
            if (!isInCrystalHollows()) return;
            if (client.player != null && client.player.swinging) {
                blockBreakTimer = ServerTick.getTime() + 5000;
            }
        });

        PlaySoundEvents.BEFORE_PLAY.register(sound -> {
            if (!ModConfigManager.get().mining.crystalHollows.powderMiningSounds) return false;
            if (!isInCrystalHollows()) return false;
            if (blockBreakTimer < ServerTick.getTime()) return false;
            if(Minecraft.getInstance().player == null) return false;

            var snd = sound.getSound();
            if (snd == null) return false;
            String path = snd.getLocation().getPath();
            return switch (path) {
                case "random/orb" -> true;
                case "block/chest/open" -> {
                    Minecraft.getInstance().player.playSound(SoundEvents.VAULT_OPEN_SHUTTER, sound.getVolume(), sound.getPitch());
                    yield true;
                }
                case "random/levelup" -> {
                    Minecraft.getInstance().player.playSound(SoundEvents.VAULT_ACTIVATE, sound.getVolume(), sound.getPitch());
                    yield true;
                }
                default -> false;
            };
        });
    }

    private static boolean isInCrystalHollows() {
        var t = HypixelLocationTracker.getInstance();
        return t.isIn("Crystal Hollows");
    }
}
