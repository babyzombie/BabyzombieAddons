package top.babyzombie.addons.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ChatUtils {

    private ChatUtils() {}

    /**
     * Strips Minecraft color codes (both § and & variants) from a string.
     */
    public static String stripColor(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-orlnm]", "").replaceAll("&[0-9a-fk-orlnm]", "");
    }

    /**
     * Sends a chat message (command) to the server.
     */
    public static void sendCommand(String command) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn != null) {
            if (command.startsWith("/")) command = command.substring(1);
            conn.sendCommand(command);
        }
    }

    /**
     * Sends a regular chat message.
     */
    public static void sendMessage(String message) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn != null) {
            conn.sendChat(message);
        }
    }

    /**
     * Removes emoji characters from a string (Unicode symbols outside BMP).
     */
    public static String removeEmoji(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            int cp = text.codePointAt(i);
            if (cp < 0x1F000 || cp > 0x1FFFF) {
                sb.appendCodePoint(cp);
            }
            if (Character.isSupplementaryCodePoint(cp)) i++;
        }
        return sb.toString();
    }

    /**
     * Extracts a regex group from a Component message string.
     */
    public static String extractPattern(Component message, java.util.regex.Pattern pattern, int group) {
        var matcher = pattern.matcher(message.getString());
        if (matcher.find()) {
            return matcher.group(group);
        }
        return null;
    }
}
