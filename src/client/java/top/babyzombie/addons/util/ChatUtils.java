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
     * Sends a regular chat message to the server.
     */
    public static void sendMessage(String message) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn != null) {
            conn.sendChat(message);
        }
    }

    /**
     * Displays a message in the client's chat HUD (not sent to server).
     */
    public static void showMessage(String message) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(message), false);
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
     * Shows a title on the HUD with timing control.
     */
    public static void showTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        var gui = Minecraft.getInstance().gui;
        gui.setTimes(fadeIn, stay, fadeOut);
        gui.setTitle(title != null ? Component.literal(title) : Component.empty());
        gui.setSubtitle(subtitle != null ? Component.literal(subtitle) : Component.empty());
    }

    /**
     * Shows a title on the HUD with default timing (0 fadeIn, 40 stay, 20 fadeOut).
     */
    public static void showTitle(String title, String subtitle) {
        showTitle(title, subtitle, 0, 40, 20);
    }

    /**
     * Shows a title on the HUD with no subtitle.
     */
    public static void showTitle(String title) {
        showTitle(title, null);
    }

    /**
     * Shows a translated title with timing control.
     */
    public static void showTranslatableTitle(String titleKey, String subtitleKey, int fadeIn, int stay, int fadeOut) {
        var gui = Minecraft.getInstance().gui;
        gui.setTimes(fadeIn, stay, fadeOut);
        gui.setTitle(titleKey != null ? Component.translatable(titleKey) : Component.empty());
        gui.setSubtitle(subtitleKey != null ? Component.translatable(subtitleKey) : Component.empty());
    }

    /**
     * Shows a translated title with no subtitle.
     */
    public static void showTranslatableTitle(String titleKey, int fadeIn, int stay, int fadeOut) {
        showTranslatableTitle(titleKey, null, fadeIn, stay, fadeOut);
    }

    /**
     * Translates a key with args through the Minecraft i18n system.
     */
    public static String translate(String key, Object... args) {
        return net.minecraft.network.chat.Component.translatable(key, args).getString();
    }

    /**
     * Copies text to the system clipboard.
     */
    public static void copyToClipboard(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
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
