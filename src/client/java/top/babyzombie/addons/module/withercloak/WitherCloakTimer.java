package top.babyzombie.addons.module.withercloak;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Tracks Wither Cloak Sword activation/deactivation and cooldown.
 */
public final class WitherCloakTimer {
    static long duration;
    static long cooldown;
    static boolean active;

    private WitherCloakTimer() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !ModConfigManager.get().witherCloak.witherCloakTimer) return;
            String text = ChatUtils.stripColor(message.getString()).trim();

            switch (text) {
                case "Creeper Veil Activated!":
                    active = true;
                    cooldown = 0;
                    duration = now();
                    break;
                case "Creeper Veil De-activated!":
                    active = false;
                    duration = 0;
                    cooldown = now() - 5000;
                    break;
                case "Not enough mana! Creeper Veil De-activated!":
                case "Creeper Veil De-activated! (Expired)":
                    active = false;
                    duration = 0;
                    cooldown = now();
                    break;
            }
        });
    }

    static long now() { return System.currentTimeMillis(); }
}
