package top.babyzombie.addons.module.misc;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.HudManager;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Moves kill combo from chat to a customizable HUD display.
 */
public final class KillComboHUD {
    static int combo;
    static long updateTime;
    static boolean active;

    private KillComboHUD() {}

    public static void init() {
        if (!ModConfigManager.get().misc.killComboHUD) return;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("Kill Combo") || (text.contains("+") && text.contains("Kill"))) {
                try {
                    String numStr = text.replaceAll("[^0-9]", "");
                    if (!numStr.isEmpty()) {
                        combo = Integer.parseInt(numStr);
                        updateTime = System.currentTimeMillis();
                        active = true;
                    }
                } catch (NumberFormatException ignored) {}
            }
        });

        HudRenderCallback.EVENT.register((gui, delta) -> {
            if (!active || System.currentTimeMillis() - updateTime > 5000) { active = false; return; }
            var font = Minecraft.getInstance().font;
            int x = HudManager.x("KillCombo"), y = HudManager.y("KillCombo");
            gui.drawString(font, "§c§lKill Combo: §f" + combo, x, y, 0xFFFFFFFF, true);
        });
    }
}
