package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

public final class MiningAbilityAlerts {
    static long readyTime;

    private MiningAbilityAlerts() {}

    public static void init() {
        // "${ability} is now available!"
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.miningAbilityAlerts) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.endsWith(" is now available!")) {
                String ability = text.substring(0, text.length() - " is now available!".length());
                readyTime = System.currentTimeMillis();
                ChatUtils.showTitle("", "§6" + ability + " §ais now available!", 0, 30, 5);
            }
        });

        // "You used your ${ability} Pickaxe Ability!"
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.miningAbilityAlerts) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.startsWith("You used your ") && text.endsWith(" Pickaxe Ability!")) {
                String ability = text.substring("You used your ".length(),
                        text.length() - " Pickaxe Ability!".length());
                ChatUtils.showTitle("", "§aYou used your §6" + ability + " §aPickaxe Ability!", 0, 20, 10);
            }
        });

        // "Your ${ability} has expired!"
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.miningAbilityAlerts) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.startsWith("Your ") && text.endsWith(" has expired!")) {
                String ability = text.substring("Your ".length(),
                        text.length() - " has expired!".length());
                ChatUtils.showTitle("", "§cYour " + ability + " has expired!", 0, 20, 10);
            }
        });
    }
}
