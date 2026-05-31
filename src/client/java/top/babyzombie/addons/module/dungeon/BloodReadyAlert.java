package top.babyzombie.addons.module.dungeon;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Announces "Blood room ready!" when The Watcher finishes spawning mobs.
 */
public final class BloodReadyAlert {
    private BloodReadyAlert() {}

    public static void init() {
        if (!ModConfigManager.get().dungeon.bloodReadyAlert) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("The Watcher") && text.contains("That will be enough")) {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§4Blood Ready!"), true);
                }
                ChatUtils.sendCommand("pc Blood room ready!");
            }
        });
    }
}
