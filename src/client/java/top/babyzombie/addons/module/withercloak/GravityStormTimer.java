package top.babyzombie.addons.module.withercloak;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

/**
 * Tracks Gravity Storm cooldown via action bar mana cost detection.
 */
public final class GravityStormTimer {
    static long time;

    private GravityStormTimer() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay || !ModConfigManager.get().witherCloak.gravityStormTimer) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            String text = ChatUtils.stripColor(message.getString());

            if (text.contains("Gravity Storm") && text.contains("Mana")) {
                time = WitherCloakTimer.now();
            }
        });
    }
}
