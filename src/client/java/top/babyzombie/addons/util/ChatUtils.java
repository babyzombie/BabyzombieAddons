package top.babyzombie.addons.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.Optional;

public final class ChatUtils {

    private ChatUtils() {}

    public static String stripColor(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-orlnm]", "").replaceAll("&[0-9a-fk-orlnm]", "");
    }

    public static void sendCommand(String command) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn != null) {
            if (command.startsWith("/")) command = command.substring(1);
            conn.sendCommand(command);
        }
    }

    public static void sendMessage(String message) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn != null) {
            conn.sendChat(message);
        }
    }

    public static void showMessage(String message) {
        showMessage(Component.literal(message));
    }

    public static void showMessage(Component message) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(message);
        }
    }

    /** Show a translated system message. */
    public static void showTranslatable(String key, Object... args) {
        showMessage(translate(key, args));
    }

    public static String stripRank(String name) {
        if (name == null) return null;
        return name.replaceFirst("^\\[[^\\]]+\\]\\s*", "");
    }

    /**
     * Remove emoji (U+1F000–U+1FFFF) and Private Use Area characters
     * (U+E000–U+F8FF, U+F0000–U+FFFFD, U+100000–U+10FFFD) from text.
     * Hypixel Skyblock server resource packs map custom icons into PUA code points;
     * stripping them lets us compare the remaining plain text.
     */
    public static String removeEmoji(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            int cp = text.codePointAt(i);
            if (!isEmojiOrPua(cp)) {
                sb.appendCodePoint(cp);
            }
            if (Character.isSupplementaryCodePoint(cp)) i++;
        }
        return sb.toString();
    }

    private static boolean isEmojiOrPua(int cp) {
        // Emoji range
        if (cp >= 0x1F000 && cp <= 0x1FFFF) return true;
        // BMP Private Use Area (U+E000–U+F8FF)
        if (cp >= 0xE000 && cp <= 0xF8FF) return true;
        // Supplementary Private Use Area-A (U+F0000–U+FFFFD)
        if (cp >= 0xF0000 && cp <= 0xFFFFD) return true;
        // Supplementary Private Use Area-B (U+100000–U+10FFFD)
        if (cp >= 0x100000 && cp <= 0x10FFFD) return true;
        return false;
    }

    public static void showTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        var client = Minecraft.getInstance();
        client.gui.setTimes(fadeIn, stay, fadeOut);
        client.gui.setTitle(Component.literal(title));
        if (subtitle != null) {
            client.gui.setSubtitle(Component.literal(subtitle));
        }
    }

    public static void showTitle(String title, String subtitle) {
        showTitle(title, subtitle, 0, 40, 20);
    }

    public static void showTitle(String title) {
        showTitle(title, null);
    }

    public static void showTranslatableTitle(String titleKey, String subtitleKey, int fadeIn, int stay, int fadeOut) {
        var client = Minecraft.getInstance();
        client.gui.setTimes(fadeIn, stay, fadeOut);
        client.gui.setTitle(Component.translatable(titleKey));
        if (subtitleKey != null) {
            client.gui.setSubtitle(Component.translatable(subtitleKey));
        }
    }

    public static void showTranslatableTitle(String titleKey, int fadeIn, int stay, int fadeOut) {
        showTranslatableTitle(titleKey, null, fadeIn, stay, fadeOut);
    }

    public static String translate(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    public static void copyToClipboard(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }

    public static String extractPattern(Component message, java.util.regex.Pattern pattern, int group) {
        var matcher = pattern.matcher(message.getString());
        if (matcher.find()) {
            return matcher.group(group);
        }
        return null;
    }

    // Manual mapping from TextColor value to legacy §-code
    // Based on standard Minecraft color codes
    private static String legacyCodeForColor(int colorValue) {
        return switch (colorValue) {
            case 0x000000 -> "§0";
            case 0x0000AA -> "§1";
            case 0x00AA00 -> "§2";
            case 0x00AAAA -> "§3";
            case 0xAA0000 -> "§4";
            case 0xAA00AA -> "§5";
            case 0xFFAA00 -> "§6";
            case 0xAAAAAA -> "§7";
            case 0x555555 -> "§8";
            case 0x5555FF -> "§9";
            case 0x55FF55 -> "§a";
            case 0x55FFFF -> "§b";
            case 0xFF5555 -> "§c";
            case 0xFF55FF -> "§d";
            case 0xFFFF55 -> "§e";
            case 0xFFFFFF -> "§f";
            default -> null;
        };
    }

    public static String toLegacyString(Component component) {
        StringBuilder sb = new StringBuilder();
        component.visit((style, str) -> {
            var color = style.getColor();
            if (color != null) {
                String legacy = legacyCodeForColor(color.getValue());
                if (legacy != null) {
                    sb.append(legacy);
                } else {
                    int rgb = color.getValue();
                    String hex = String.format("%06X", rgb);
                    sb.append("§x");
                    for (char c : hex.toCharArray()) {
                        sb.append("§").append(c);
                    }
                }
            }
            if (style.isBold()) sb.append("§l");
            if (style.isItalic()) sb.append("§o");
            if (style.isUnderlined()) sb.append("§n");
            if (style.isStrikethrough()) sb.append("§m");
            if (style.isObfuscated()) sb.append("§k");
            sb.append(str);
            return Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }

    public static String formatTime(long ms) {
        long s = ms / 1000, m = (ms % 1000) / 10;
        return String.format("%d.%02ds", s, m);
    }
}
