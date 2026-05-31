package top.babyzombie.addons.module.party;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Party management: auto-accept invites, auto-accept reparty,
 * double P-warp confirm, party chat commands.
 */
public final class PartyModule {

    private static final Pattern INVITE_PATTERN = Pattern.compile("(.+) has invited you to join their party");
    private static final Pattern DISBAND_PATTERN = Pattern.compile("The party was disbanded");
    private static final Pattern WARP_CONFIRM_PATTERN = Pattern.compile("Are you sure.*warp");
    private static boolean partyDisbanded;

    private PartyModule() {}

    public static void init() {
        var config = ModConfigManager.get().party;

        // Auto-accept party invites
        if (config.autoAccept) {
            ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
                if (overlay) return;
                String text = message.getString();
                Matcher m = INVITE_PATTERN.matcher(ChatUtils.stripColor(text));
                if (m.find()) {
                    ChatUtils.sendCommand("party accept " + m.group(1));
                }
            });
        }

        // Double P-warp confirm
        if (config.doublePWarpConfirm) {
            ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
                if (overlay) return;
                String text = ChatUtils.stripColor(message.getString());
                if (text.contains("Are you sure") && text.contains("/p warp")) {
                    ChatUtils.sendCommand("p warp");
                }
            });
        }

        // Party chat commands (!ptme, !allinv, !p <player>, !warp, !sc, etc.)
        if (config.partyCommands) {
            ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
                if (overlay) return;
                String text = ChatUtils.stripColor(message.getString());

                if (text.startsWith("Party >")) {
                    String cmd = text.substring("Party >".length()).trim();
                    handlePartyCommand(cmd);
                }
            });
        }
    }

    private static void handlePartyCommand(String cmd) {
        String[] parts = cmd.split(" ");
        String command = parts[0].toLowerCase();

        switch (command) {
            case "!ptme":
                ChatUtils.sendCommand("p transfer " + Minecraft.getInstance().player.getName().getString());
                break;
            case "!allinv":
                ChatUtils.sendCommand("p settings allinvite");
                break;
            case "!p":
                if (parts.length > 1) {
                    ChatUtils.sendCommand("p " + parts[1]);
                }
                break;
            case "!warp":
                ChatUtils.sendCommand("p warp");
                break;
            case "!sc":
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    var pos = player.blockPosition();
                    String coords = String.format("x=%d y=%d z=%d", pos.getX(), pos.getY(), pos.getZ());
                    ChatUtils.sendCommand("pc " + coords);
                }
                break;
            case "!join":
                if (parts.length > 1) {
                    ChatUtils.sendCommand("joindungeon " + parts[1]);
                }
                break;
            case "!c":
                // Cancel warp
                ChatUtils.sendCommand("pc Warp cancelled!");
                break;
        }
    }
}
