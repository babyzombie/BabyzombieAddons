package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * 检测聊天栏出现 "YOU COCOONED YOUR SLAYER BOSS" 时，
 * 发出 Title 和声音提示 Boss 已复活。
 */
public final class SlayerBossRespawnAlert {
    private static final String COCOON_MESSAGE = "  YOU COCOONED YOUR SLAYER BOSS";

    private SlayerBossRespawnAlert() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().slayer.bossRespawnAlert) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

            String text = ChatUtils.stripColor(message.getString()).trim();
            if (text.equals(COCOON_MESSAGE)) {
                ChatUtils.showTranslatableTitle("slayer.bossRespawnAlert.title", "slayer.bossRespawnAlert.subtitle", 0, 50, 10);

                var player = Minecraft.getInstance().player;
                if (player != null) {
                    player.level().playSound(player, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.MASTER, 1f, 1f);
                }
            }
        });
    }
}
