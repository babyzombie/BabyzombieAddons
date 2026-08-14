package top.babyzombie.addons.module.fishing;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.PlaySoundHelper;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.Objects;

public final class MuteVanquisher {
    private MuteVanquisher() {};

    public static void init() {
        PlaySoundEvents.MODIFY.register((sound) -> {
            if (!HypixelLocationTracker.getInstance().isInCrimson()) return sound;
            var item = Objects.requireNonNull(Minecraft.getInstance().player).getMainHandItem();
            if (!item.is(Items.FISHING_ROD)) return sound;
            var name = sound.getIdentifier().getPath();
            var cfg = ModConfigManager.get().fishing.muteVanquisher;
            if (name.startsWith("mob/wither/spawn") && cfg.spawn < 1) {
                return PlaySoundHelper.withVolume(sound, cfg.spawn);
            } else if (name.startsWith("mob/wither/idle") && cfg.idle < 1) {
                return PlaySoundHelper.withVolume(sound, cfg.idle);
            } else if (name.startsWith("mob/wither/hurt") && cfg.hurt < 1) {
                return PlaySoundHelper.withVolume(sound, cfg.hurt);
            } else if (name.startsWith("mob/wither/shoot") && cfg.shoot < 1) {
                return PlaySoundHelper.withVolume(sound, cfg.shoot);
            } else if (name.startsWith("mob/wither/death") && cfg.death < 1) {
                return PlaySoundHelper.withVolume(sound, cfg.death);
            }
            return sound;
        });
    }
}
