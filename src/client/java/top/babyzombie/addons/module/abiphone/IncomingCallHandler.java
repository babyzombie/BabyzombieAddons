package top.babyzombie.addons.module.abiphone;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.SendCommandEvents;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IncomingCallHandler {

    private static final Pattern CALLER_PATTERN = Pattern.compile("✆\\s*(.+?)\\s*✆");
    private static String pendingCaller;

    public static void register() {
        ClientReceiveMessageEvents.GAME.register(IncomingCallHandler::onGameMessage);
        SendCommandEvents.BEFORE_SEND.register(command -> {
            if (!HypixelLocationTracker.getInstance().isOnHypixel()) return false;
            if (command.equals("call")
                    && HypixelLocationTracker.getInstance().isInSkyblock()
                    && ModConfigManager.get().misc.abiphoneGui) {
                var client = Minecraft.getInstance();
                var tracker = HypixelLocationTracker.getInstance();
                var contacts = AbiphoneTracker.getInstance()
                        .loadItems(tracker.getUuid(), tracker.getProfileId());
                client.execute(() -> client.setScreenAndShow(new AbiphoneContactScreen(contacts)));
                return true;
            }
            return false;
        });
    }

    private static void onGameMessage(Component message, boolean overlay) {
        if (overlay) return;

        String text = message.getString();

        // Message 1: ✆ <name> ✆  (caller announcement)
        if (text.contains("✆") && !text.contains("[PICK UP]")) {
            String name = extractCaller(text);
            if (name != null) {
                pendingCaller = name;
            }
            return;
        }

        // Message 2: ✆ BUZZ... [PICK UP]  (pickup prompt)
        if (text.contains("[PICK UP]") && pendingCaller != null) {
            String caller = pendingCaller;
            pendingCaller = null;

            Set<String> autoAnswerNames = AbiphoneContactScreen.getAutoAnswerNames();
            if (!autoAnswerNames.contains(caller)) return;

            ClickEvent clickEvent = findClickEvent(message);
            if (!(clickEvent instanceof ClickEvent.RunCommand runCommand)) return;

            String command = runCommand.command();
            if (command.startsWith("/")) command = command.substring(1);

            var conn = Minecraft.getInstance().getConnection();
            if (conn != null) {
                conn.sendCommand(command);
            }
        }
    }

    private static String extractCaller(String text) {
        Matcher m = CALLER_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        int idx = text.indexOf("✆");
        if (idx >= 0) {
            String after = text.substring(idx + 1).trim();
            int space = after.indexOf(' ');
            if (space > 0) return after.substring(0, space);
        }
        return null;
    }

    private static ClickEvent findClickEvent(Component component) {
        String s = component.getString();
        if (s.contains("[PICK UP]")) {
            ClickEvent ce = clickEventInStyle(component.getStyle());
            if (ce != null) return ce;
        }
        List<Component> siblings = component.getSiblings();
        for (Component child : siblings) {
            ClickEvent ce = findClickEvent(child);
            if (ce != null) return ce;
        }
        return null;
    }

    private static ClickEvent clickEventInStyle(Style style) {
        if (style == null) return null;
        return style.getClickEvent();
    }
}
