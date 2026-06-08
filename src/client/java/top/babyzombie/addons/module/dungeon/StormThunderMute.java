package top.babyzombie.addons.module.dungeon;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.Scheduler;

/**
 * Mutes thunder sounds for 22 seconds after Storm's death message in F7.
 */
public final class StormThunderMute {

    private StormThunderMute() {}

    public static void init() {
        final boolean[] mutingStorm = {false};

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().dungeon.muteStormThunder) return;
            if (!HypixelLocationTracker.getInstance().isInDungeon()) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.equals("[BOSS] Storm: I should have known that I stood no chance.")) {
                mutingStorm[0] = true;
                Scheduler.schedule(440, () -> mutingStorm[0] = false);
            }
        });

        PlaySoundEvents.BEFORE_PLAY.register(sound -> {
            if (!mutingStorm[0]) return false;
            if (!ModConfigManager.get().dungeon.muteStormThunder) return false;
            var snd = sound.getSound();
            if (snd == null) return false;
            return snd.getLocation().getPath().contains("ambient/weather/thunder");
        });
    }
}
