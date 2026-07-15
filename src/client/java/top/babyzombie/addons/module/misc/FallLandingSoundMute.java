package top.babyzombie.addons.module.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * 修改自己的落地声音量（GENERIC_SMALL_FALL / GENERIC_BIG_FALL）。
 * 仅 SkyBlock 内生效，通过配置滑块 {@code skyblock.fallSoundVolume} 调节。
 */
public final class FallLandingSoundMute {
    private FallLandingSoundMute() {}

    private static final double SELF_RADIUS = 1.0;

    public static void init() {
        PlaySoundEvents.MODIFY.register(sound -> {
            // 仅 SkyBlock
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return sound;

            var cfg = ModConfigManager.get().skyblock;
            if (cfg.fallSoundVolume >= 1.0f) return sound;

            var snd = sound.getSound();
            if (snd == null) return sound;

            // 只处理实体落地声
            var loc = snd.getLocation();
            if (!loc.getPath().equals("damage/fallsmall")
                    && !loc.getPath().equals("damage/fallbig")) {
                return sound;
            }

            // 只处理自己的声音
            Player player = Minecraft.getInstance().player;
            if (player == null) return sound;
            double dx = sound.getX() - player.getX();
            double dy = sound.getY() - player.getY();
            double dz = sound.getZ() - player.getZ();
            if (dx * dx + dy * dy + dz * dz > SELF_RADIUS * SELF_RADIUS) return sound;

            var resolved = sound.getSound();
            return new SimpleSoundInstance(
                    sound.getIdentifier(), sound.getSource(), cfg.fallSoundVolume, sound.getPitch(),
                    RandomSource.create(), sound.isLooping(), sound.getDelay(), sound.getAttenuation(),
                    sound.getX(), sound.getY(), sound.getZ(), sound.isRelative()
            ) {{
                this.sound = resolved;
            }
            @Override public float getVolume() { return cfg.fallSoundVolume; }};
        });
    }
}
