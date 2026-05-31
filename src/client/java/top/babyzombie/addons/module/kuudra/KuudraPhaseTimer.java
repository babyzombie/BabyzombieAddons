package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.HudManager;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Tracks and displays current Kuudra phase and elapsed time.
 */
public final class KuudraPhaseTimer {
    static String currentPhase = "";
    static long phaseStartTime;

    private KuudraPhaseTimer() {}

    public static void init() {
        if (!ModConfigManager.get().kuudra.phaseTimer) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !KuudraHPDisplay.inKuudra) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("Supplies")) { currentPhase = "Supplies"; phaseStartTime = now(); }
            if (text.contains("Ballista")) { currentPhase = "Ballista"; phaseStartTime = now(); }
            if (text.contains("Stun")) { currentPhase = "Stun"; phaseStartTime = now(); }
            if (text.contains("True Lair") || text.contains("Final Phase")) {
                currentPhase = "True Lair"; phaseStartTime = now();
            }
        });

        HudRenderCallback.EVENT.register((gui, delta) -> {
            if (currentPhase.isEmpty() || !KuudraHPDisplay.inKuudra) return;
            long elapsed = now() - phaseStartTime;
            long s = elapsed / 1000 % 60, m = elapsed / 60000;
            var font = Minecraft.getInstance().font;
            int x = HudManager.x("KuudraHP"), y = HudManager.y("KuudraHP") + 12;
            gui.drawString(font, String.format("§ePhase: §a%s §7(%d:%02d)", currentPhase, m, s),
                    x, y, 0xFFFFFFFF, true);
        });
    }

    private static long now() { return System.currentTimeMillis(); }
}
