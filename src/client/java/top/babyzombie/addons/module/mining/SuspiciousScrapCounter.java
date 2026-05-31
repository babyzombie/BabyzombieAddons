package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.HudManager;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Counts suspicious scrap pickups and shows HUD.
 */
public final class SuspiciousScrapCounter {
    static int count;

    private SuspiciousScrapCounter() {}

    public static void init() {
        if (!ModConfigManager.get().mining.suspiciousScrapCounter) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("Suspicious Scrap")) count++;
        });

        HudRenderCallback.EVENT.register((gui, delta) -> {
            if (count <= 0) return;
            var font = Minecraft.getInstance().font;
            int x = HudManager.x("SuspiciousScrap"), y = HudManager.y("SuspiciousScrap");
            gui.drawString(font, "§6Scraps: §f" + count, x, y, 0xFFFFFFFF, true);
        });
    }
}
