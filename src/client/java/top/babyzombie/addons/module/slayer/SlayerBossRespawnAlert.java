package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.PlaySoundHelper;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * 检测聊天栏出现 "YOU COCOONED YOUR SLAYER BOSS" 时，
 * 发出 Title 和声音提示 Boss 已复活。
 */
public final class SlayerBossRespawnAlert {
    private static final String COCOON_MESSAGE = "  YOU COCOONED YOUR SLAYER BOSS";

    private SlayerBossRespawnAlert() {}

    public static void init() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            if (!ModConfigManager.get().slayer.bossRespawnAlert) return true;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return true;

            String text = ChatUtils.stripColor(message.getString());
            if (text.equals(COCOON_MESSAGE)) {
                ChatUtils.showTranslatableTitle("slayer.bossRespawnAlert.title", "slayer.bossRespawnAlert.subtitle", 0, 50, 10);

                PlaySoundHelper.playSeeked(SimpleSoundInstance.forUI(SoundEvents.MUSIC_DISC_PIGSTEP, 1), 23, 5.8f);
            }
            return true;
        });
    }
}
