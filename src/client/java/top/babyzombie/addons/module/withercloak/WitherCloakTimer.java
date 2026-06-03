package top.babyzombie.addons.module.withercloak;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.DungeonCooldown;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

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
                    duration = ServerTick.getTime();
                    break;
                case "Creeper Veil De-activated!":
                    active = false;
                    duration = 0;
                    cooldown = calcCooldown(ServerTick.getTime() - 5000, 5000);
                    break;
                case "Not enough mana! Creeper Veil De-activated!":
                case "Creeper Veil De-activated! (Expired)":
                    active = false;
                    duration = 0;
                    cooldown = calcCooldown(ServerTick.getTime(), 10000);
                    break;
            }
        });
    }

    private static long calcCooldown(long time, long baseCd) {
        if (HypixelLocationTracker.getInstance().isInDungeon())
            return DungeonCooldown.calculate(time, baseCd);
        return time;
    }
}
