package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.ModConfigManager;

import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

public final class SuspiciousScrapCounter {
    static int count;

    private SuspiciousScrapCounter() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.suspiciousScrapCounter) return;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock() || !"Mineshaft".equals(tracker.getLocation())) return;
            if (ChatUtils.stripColor(message.getString()).startsWith("EXCAVATOR! You found a Suspicious Scrap!")) {
                count++;
            }
        });

        HudRenderCallback.EVENT.register((gui, delta) -> {
            if (!ModConfigManager.get().mining.suspiciousScrapCounter) return;
            if (count <= 0) return;
            var font = Minecraft.getInstance().font;
            int x = HudManager.x("SuspiciousScrap"), y = HudManager.y("SuspiciousScrap");
            float s = HudManager.scale("SuspiciousScrap");
            String color = count >= 5 ? "§a" : "§e";
            HudManager.drawScaled(gui, font, "§6Scraps: " + color + count + "/5", x, y, s);
        });
    }
}
