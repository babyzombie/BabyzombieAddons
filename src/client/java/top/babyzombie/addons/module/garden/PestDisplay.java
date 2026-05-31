package top.babyzombie.addons.module.garden;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Shows alerts when pests spawn in the Garden.
 */
public final class PestDisplay {
    private PestDisplay() {}

    public static void init() {
        if (!ModConfigManager.get().garden.pestDisplay) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("Pest") && (text.contains("spawned") || text.contains("appeared"))) {
                var player = Minecraft.getInstance().player;
                if (player != null)
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c§lPest Spawned! §e" + text), true);
            }
        });
    }
}
