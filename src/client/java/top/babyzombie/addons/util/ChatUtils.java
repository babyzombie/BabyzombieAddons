package top.babyzombie.addons.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import top.babyzombie.addons.util.toast.ItemToast;

public final class ChatUtils {

    private ChatUtils() {}

    public static String stripColor(String text) {
        if (text == null) return "";
        // §x/&x 六位 hex 染色（如 §x§F§F§C§E§4§7）需整段剥离，优先级高于单字符染色码
        return text
                .replaceAll("§x(?:§[0-9a-fA-F]){6}|§[0-9a-fk-orlnm]", "")
                .replaceAll("&x(?:&[0-9a-fA-F]){6}|&[0-9a-fk-orlnm]", "");
    }

    /** 把输入的 & 颜色码转换为 § 形式（含 &x 六位 hex），用于"用 & 代替 §"的文本输入。 */
    public static String ampToSection(String text) {
        if (text == null) return "";
        Matcher hex = Pattern.compile("&x((?:&[0-9a-fA-F]){6})").matcher(text);
        StringBuilder sb = new StringBuilder();
        while (hex.find()) {
            hex.appendReplacement(sb, "§x" + hex.group(1).replace("&", "§"));
        }
        hex.appendTail(sb);
        return sb.toString().replaceAll("&([0-9a-fk-orlnm])", "§$1");
    }

    /** 把 § 颜色码转换为 & 形式（含 §x 六位 hex），用于编辑框回显。 */
    public static String sectionToAmp(String text) {
        if (text == null) return "";
        Matcher hex = Pattern.compile("§x((?:§[0-9a-fA-F]){6})").matcher(text);
        StringBuilder sb = new StringBuilder();
        while (hex.find()) {
            hex.appendReplacement(sb, "&x" + hex.group(1).replace("§", "&"));
        }
        hex.appendTail(sb);
        return sb.toString().replaceAll("§([0-9a-fk-orlnm])", "&$1");
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
     * Remove emoji, decorative symbol blocks and Private Use Area characters
     * from text.
     * <p>
     * Covers:
     * <ul>
     *   <li>Emoji &amp; pictographs (U+1F000–U+1FFFF, incl. U+1F600–U+1F64F, U+1F900–U+1F9FF)</li>
     *   <li>Decorative symbol blocks servers map icons into:
     *       arrows (U+2190–U+21FF), misc technical (U+2300–U+23FF),
     *       geometric shapes (U+25A0–U+25FF), misc symbols (U+2600–U+26FF, e.g. ♔),
     *       dingbats (U+2700–U+27BF), misc symbols &amp; arrows (U+2B00–U+2BFF),
     *       variation selectors (U+FE00–U+FE0F)</li>
     *   <li>Private Use Area (U+E000–U+F8FF, U+F0000–U+FFFFD, U+100000–U+10FFFD)</li>
     * </ul>
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
        // Arrows (U+2190–U+21FF)
        if (cp >= 0x2190 && cp <= 0x21FF) return true;
        // Miscellaneous Technical (U+2300–U+23FF)
        if (cp >= 0x2300 && cp <= 0x23FF) return true;
        // Geometric Shapes (U+25A0–U+25FF)
        if (cp >= 0x25A0 && cp <= 0x25FF) return true;
        // Miscellaneous Symbols (U+2600–U+26FF) — includes ♔ (U+2654)
        if (cp >= 0x2600 && cp <= 0x26FF) return true;
        // Dingbats (U+2700–U+27BF)
        if (cp >= 0x2700 && cp <= 0x27BF) return true;
        // Miscellaneous Symbols and Arrows (U+2B00–U+2BFF)
        if (cp >= 0x2B00 && cp <= 0x2BFF) return true;
        // Variation Selectors (U+FE00–U+FE0F)
        if (cp >= 0xFE00 && cp <= 0xFE0F) return true;
        // Emoji range (U+1F000–U+1FFFF)
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
        client.gui.hud.setTimes(fadeIn, stay, fadeOut);
        client.gui.hud.setTitle(Component.literal(title));
        if (subtitle != null) {
            client.gui.hud.setSubtitle(Component.literal(subtitle));
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
        client.gui.hud.setTimes(fadeIn, stay, fadeOut);
        client.gui.hud.setTitle(Component.translatable(titleKey));
        if (subtitleKey != null) {
            client.gui.hud.setSubtitle(Component.translatable(subtitleKey));
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

    /** 自建 id,避免与原版/其他 mod 的 SystemToast 撞 token */
    private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId();

    public static void showToast(String titleKey, String bodyKey, Object... bodyArgs) {
        showToast(Component.translatable(titleKey), Component.translatable(bodyKey, bodyArgs));
    }

    public static void showToast(Component title, Component body) {
        try {
            Minecraft mc = Minecraft.getInstance();
            mc.gui.toastManager().addToast(
                    new SystemToast(SystemToast.SystemToastId.NARRATOR_TOGGLE, title, body)
            );
        } catch (Throwable ignored) {
            // 静默失败,不影响主流程
        }
    }

    /** 带物品图标的 Toast,正文超长自动折行;同样排队显示不覆盖 */
    public static void showToast(ItemStack icon, String titleKey, String bodyKey, Object... bodyArgs) {
        showToast(icon, Component.translatable(titleKey), Component.translatable(bodyKey, bodyArgs));
    }

    /** 带物品图标的 Toast,直接传 Component(如物品 hoverName);排队显示不覆盖 */
    public static void showToast(ItemStack icon, Component title, Component message) {
        try {
            Minecraft mc = Minecraft.getInstance();
            mc.gui.toastManager().addToast(new ItemToast(icon, title, message));
        } catch (Throwable ignored) {
            // 静默失败,不影响复制主流程
        }
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
