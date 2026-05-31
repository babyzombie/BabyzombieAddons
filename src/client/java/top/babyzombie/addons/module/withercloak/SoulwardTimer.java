package top.babyzombie.addons.module.withercloak;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

/**
 * Tracks Soulward ability duration and cooldown via action bar mana cost detection.
 */
public final class SoulwardTimer {
    static long duration;
    static long cooldown;

    private SoulwardTimer() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay || !ModConfigManager.get().witherCloak.soulwardTimer) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            String text = ChatUtils.stripColor(message.getString());

            if (text.contains("Soulward") && text.contains("Mana")) {
                cooldown = WitherCloakTimer.now();
                duration = WitherCloakTimer.now();
            }
        });
    }
}
