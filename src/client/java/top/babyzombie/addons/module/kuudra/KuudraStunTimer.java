package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

public final class KuudraStunTimer {
    private KuudraStunTimer() {}

    private static long stunEnd;
    private static long downEnd;
    private static long p4End;

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!ModConfigManager.get().kuudra.stunTimer) return;
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return;

            if (message.getString().contains("destroyed one of Kuudra's pods")) {
                var loc = HypixelLocationTracker.getInstance().getLocation();
                if (loc == null) return;
                int duration = loc.contains("T5") ? 8000 : (loc.contains("T3") ? 12000 : 10000);
                stunEnd = ServerTick.getTime() + duration;
            }

            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("POW! SURELY THAT'S IT")) {
                stunEnd = 0;
                KuudraLocationTracker.p4 = true;
                var loc = HypixelLocationTracker.getInstance().getLocation();
                if (loc != null && loc.contains("T5")) {
                    p4End = ServerTick.getTime() + 3000;
                } else {
                    downEnd = ServerTick.getTime() + 15300;
                }
            }
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_stun_timer"),
                (context, tickCounter) -> {
            if (!ModConfigManager.get().kuudra.stunTimer) return;
            long now = ServerTick.getTime();
            var font = Minecraft.getInstance().font;

            String text = null;
            if (stunEnd > now) {
                text = ChatUtils.translate("kuudra.stun.stunned", formatTime(stunEnd - now));
            } else if (downEnd > now) {
                text = ChatUtils.translate("kuudra.stun.down", formatTime(downEnd - now));
            } else if (p4End > now) {
                text = ChatUtils.translate("kuudra.stun.p4");
            }

            if (text != null) {
                int x = HudManager.x("KuudraStun"), y = HudManager.y("KuudraStun");
                float s = HudManager.scale("KuudraStun");
                HudManager.drawScaled(context, font, text, x, y, s);
            }
        });
    }

    private static String formatTime(long ms) {
        long s = ms / 1000, m = s / 60;
        s %= 60;
        return String.format("%d:%02d", m, s);
    }
}
