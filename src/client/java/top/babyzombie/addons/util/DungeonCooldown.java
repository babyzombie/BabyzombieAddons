package top.babyzombie.addons.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

/**
 * Dungeon mage cooldown reduction, matching the JS InDungeonCooldown logic.
 * Scans the tab list for the player's mage class level and parses the
 * Roman numeral level. Listens for [Mage] Cooldown Reduction messages
 * to track duplicate mage status.
 */
public final class DungeonCooldown {
    private static boolean nodupeMage;

    private DungeonCooldown() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.startsWith("[Mage] Cooldown Reduction")) {
                nodupeMage = true;
            }
        });

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
            if (world != null) nodupeMage = false;
        });
    }

    /**
     * Applies dungeon mage cooldown reduction if the player is a mage in dungeons.
     * @param time  current ServerTick time
     * @param cd    base cooldown in milliseconds
     * @return      reduced cooldown time
     */
    public static long calculate(long time, long cd) {
        int level = getMageLevel();
        if (level < 0) return time;

        int halfLevel = level / 2;
        int reduction = nodupeMage
                ? (50 + halfLevel) * (int)(cd / 100)
                : (25 + halfLevel) * (int)(cd / 100);
        return time - reduction;
    }

    private static int getMageLevel() {
        var client = Minecraft.getInstance();
        var connection = client.getConnection();
        if (connection == null) return -1;
        var player = client.player;
        if (player == null) return -1;
        String playerName = player.getName().getString();

        for (var info : connection.getOnlinePlayers()) {
            var displayName = info.getTabListDisplayName();
            if (displayName == null) continue;
            String name = ChatUtils.stripColor(displayName.getString());
            if (name.contains(playerName) && name.contains("Mage")) {
                String[] parts = name.split(" ");
                String last = parts[parts.length - 1];
                last = last.replaceAll("[\\[\\]()]", "");
                return parseRoman(last);
            }
        }
        return -1;
    }

    private static int parseRoman(String s) {
        s = s.toUpperCase();
        int result = 0;
        for (int i = 0; i < s.length(); i++) {
            int cur = romanValue(s.charAt(i));
            if (cur == 0) continue;
            int next = i + 1 < s.length() ? romanValue(s.charAt(i + 1)) : 0;
            if (cur < next) {
                result += next - cur;
                i++;
            } else {
                result += cur;
            }
        }
        return result > 0 ? result : -1;
    }

    private static int romanValue(char c) {
        return switch (c) {
            case 'I' -> 1;
            case 'V' -> 5;
            case 'X' -> 10;
            case 'L' -> 50;
            case 'C' -> 100;
            case 'D' -> 500;
            case 'M' -> 1000;
            default -> 0;
        };
    }
}
