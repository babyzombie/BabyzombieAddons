package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.ModConfigManager;

import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

public final class ScathaCooldown {
    static long time;

    private ScathaCooldown() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.scathaCooldown) return;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock() || !"Crystal Hollows".equals(tracker.getMap())) return;
            if (ChatUtils.stripColor(message.getString()).equals("You hear the sound of something approaching...")) {
                time = ServerTick.getTime();
            }
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "scatha_cooldown"),
                (context, tickCounter) -> {
            if (!ModConfigManager.get().mining.scathaCooldown) return;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock() || !"Crystal Hollows".equals(tracker.getMap())) return;
            if (time == 0) return;

            long elapsed = ServerTick.getTime() - time;
            if (elapsed > 30_000) return;

            var font = Minecraft.getInstance().font;
            int x = HudManager.x("ScathaCooldown"), y = HudManager.y("ScathaCooldown");
            float s = HudManager.scale("ScathaCooldown");
            long remaining = 30_000 - elapsed;
            String text = String.format("§5§lScatha: §8§l%d.%03ds", remaining / 1000, remaining % 1000);
            HudManager.drawScaled(context, font, text, x, y, s);
        });
    }
}
