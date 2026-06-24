package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.ModConfigManager;

import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

public final class SuspiciousScrapCounter {
    static int count;

    private SuspiciousScrapCounter() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.suspiciousScrapCounter) return;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isIn("Mineshaft")) return;
            if (ChatUtils.stripColor(message.getString()).startsWith("EXCAVATOR! You found a Suspicious Scrap!")) {
                count++;
            }
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "suspicious_scrap"),
                (context, tickCounter) -> {
            if (!ModConfigManager.get().mining.suspiciousScrapCounter) return;
            if (count <= 0) return;
            var font = Minecraft.getInstance().font;
            int x = HudManager.x("SuspiciousScrap"), y = HudManager.y("SuspiciousScrap");
            float s = HudManager.scale("SuspiciousScrap");
            String color = count >= 5 ? "§a" : "§e";
            HudManager.drawScaled(context, font, "§6Scraps: " + color + count + "/5", x, y, s);
        });
    }
}
