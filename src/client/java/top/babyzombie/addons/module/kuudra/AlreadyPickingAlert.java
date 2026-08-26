package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * Already Picking — 当别人正在捡你面前的补给时，显示 title 提示 + 播放音效。
 */
public final class AlreadyPickingAlert {
    private AlreadyPickingAlert() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!ModConfigManager.get().kuudra.phase1.alreadyPickingAlert) return;
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return;

            String text = ChatUtils.stripColor(message.getString());
            if (text.startsWith("Someone else is currently trying to pick up these supplies")) {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    ChatUtils.showTitle("", "§c§lALREADY PICKING", 0, 30, 10);
                    player.level().playSound(player, player.blockPosition(),
                            SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1f, 1f);
                }
            }
        });
    }
}
