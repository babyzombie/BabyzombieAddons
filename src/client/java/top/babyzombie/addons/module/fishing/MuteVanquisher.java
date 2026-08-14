package top.babyzombie.addons.module.fishing;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.PlaySoundHelper;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

public final class MuteVanquisher {
    private MuteVanquisher() {};

    public static void init() {
        PlaySoundEvents.MODIFY.register((sound) -> {
            if (!HypixelLocationTracker.getInstance().isInCrimson()) return sound;
            if (Minecraft.getInstance().player == null) return sound;
            ItemStack item = Minecraft.getInstance().player.getMainHandItem();
            if (!item.is(Items.FISHING_ROD)) return sound;
            var soundIdentifier  = sound.getIdentifier();
            var cfg = ModConfigManager.get().fishing.muteVanquisher;
            if (soundIdentifier.equals(SoundEvents.WITHER_SPAWN.location()) && cfg.spawn < 1) {
                return PlaySoundHelper.withVolume(sound, cfg.spawn);
            } else if (soundIdentifier.equals(SoundEvents.WITHER_AMBIENT.location()) && cfg.idle < 1) {
                return PlaySoundHelper.withVolume(sound, cfg.idle);
            } else if (soundIdentifier.equals(SoundEvents.WITHER_HURT.location()) && cfg.hurt < 1) {
                return PlaySoundHelper.withVolume(sound, cfg.hurt);
            } else if (soundIdentifier.equals(SoundEvents.WITHER_SHOOT.location()) && cfg.shoot < 1) {
                return PlaySoundHelper.withVolume(sound, cfg.shoot);
            } else if (soundIdentifier.equals(SoundEvents.WITHER_DEATH.location()) && cfg.death < 1) {
                return PlaySoundHelper.withVolume(sound, cfg.death);
            }
            return sound;
        });
    }
}
