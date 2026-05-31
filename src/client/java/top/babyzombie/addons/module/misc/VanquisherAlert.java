package top.babyzombie.addons.module.misc;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Auto-sends Vanquisher coordinates to party chat when one spawns.
 */
public final class VanquisherAlert {
    private VanquisherAlert() {}

    public static void init() {
        if (!ModConfigManager.get().general.vanquisherAlert) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("Vanquisher") && text.contains("spawned")) {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    var pos = player.blockPosition();
                    ChatUtils.sendCommand("pc Vanquisher at " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
                }
            }
        });
    }
}
