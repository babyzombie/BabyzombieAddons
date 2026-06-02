package top.babyzombie.addons.module.party;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.PartyTracker;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Party management: auto-accept invites, double P-warp, party chat commands.
 * Original regex patterns preserved from ChatTriggers JS.
 */
public final class PartyModule {

    // Same multi-line party invite regex used by PopupEventsModule
    private static final Pattern PARTY_INVITE = Pattern.compile(
            ".*(\\[[\\w+\\+-]+] )?([0-9a-zA-Z_]{2,24})( has invited you to join | has invited all members of .+ to |邀请你加入|已邀请.+中的所有成员加入)(.+)( party!|组队！).*",
            Pattern.DOTALL);

    // Party chat line: "Party > [RANK] PlayerName: message" (5 capture groups matching original JS)
    public static final Pattern PARTY_CHAT = Pattern.compile(
            "(组队|組隊|Party) > (\\[[\\w+\\+-]+] )?([0-9a-zA-Z_]+)( [♲Ⓑ☀⚒ቾ]+)?: (.+)");

    // DM message: "From PlayerName: !p"
    private static final Pattern DM_INVITE = Pattern.compile(
            "^From (.+): [!！][ ]?p(?:arty)?(?: .*)?$", Pattern.CASE_INSENSITIVE);

    // Party chat commands
    private static final Pattern CMD_ALLINVITE = Pattern.compile("^[!！][ ]?all(?:inv)?(?:ite)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_PINVITE = Pattern.compile("^[!！][ ]?p ([a-zA-Z0-9_-]+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_WARP = Pattern.compile("^[!！][ ]?warp?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_WARP_CANCEL = Pattern.compile("^[!！][ ]?(?:warp)?[ ]?c(?:ancel)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_JOIN = Pattern.compile("^[!！][ ]?(?:join)?[ ]?([fmt])([e0-7])$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_SENDCOORDS = Pattern.compile("^[!！][ ]?(?:s)?(?:end)?[ ]?c(?:oord|oords)?$", Pattern.CASE_INSENSITIVE);

    private static boolean partyDisbanded;
    private static long warpDelayUntil;
    private static String nextCommand;
    private static final Map<String, Long> dmInvitePending = new HashMap<>();

    private PartyModule() {}

    public static void init() {

        // Auto-accept party invite
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().party.dmPartyInvite) return;
            String text = message.getString();
            long now = System.currentTimeMillis();
            dmInvitePending.values().removeIf(t -> t < now);

            var m = PARTY_INVITE.matcher(text);
            if (!m.find()) return;

            // Only auto-accept if inviter was recently sent DM !p by us
            String inviter = m.group(2);
            if (inviter != null && dmInvitePending.containsKey(inviter.toLowerCase())) {
                dmInvitePending.remove(inviter.toLowerCase());
                ChatUtils.sendCommand("party accept");
                showMsg("party.accepted");
            }
        });

        // Detect party disband for reparty context
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("disbanded the party") || text.contains("解散了组队")) {
                partyDisbanded = true;
                // Reset after 2 minutes
                long now = System.currentTimeMillis();
                new Thread(() -> { try { Thread.sleep(120000); partyDisbanded = false; } catch (Exception ignored) {} }).start();
            }
        });

        // Party chat commands
            ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
                if (overlay) return;
                var cfg = ModConfigManager.get().party;
                String text = message.getString();
                var matcher = PARTY_CHAT.matcher(text);
                if (!matcher.find()) return;

                String player = matcher.group(3);
                String msg = ChatUtils.stripColor(matcher.group(5)).trim();

                // Don't respond to self
                var self = Minecraft.getInstance().player;
                if (self != null && player.equals(self.getName().getString())) return;

                // !allinv → toggle allinvite
                if (cfg.partyAllinvite && CMD_ALLINVITE.matcher(msg).matches()) {
                    nextCommand = "party settings allinvite";
                    runWhenLeader();
                    return;
                }

                // !p <player> → invite
                if (cfg.partyInvite && CMD_PINVITE.matcher(msg).matches()) {
                    var pm = CMD_PINVITE.matcher(msg);
                    if (pm.find()) {
                        nextCommand = "party invite " + pm.group(1);
                        runWhenLeader();
                    }
                    return;
                }

                // !warp → warp
                if (cfg.partyWarp && CMD_WARP.matcher(msg).matches()) {
                    if (cfg.partyWarpDelay) {
                        warpDelayUntil = System.currentTimeMillis() + cfg.partyWarpDelaySeconds * 1000L;
                        return;
                    }
                    nextCommand = "party warp";
                    runWhenLeader();
                    return;
                }

                // !c / !warp cancel → cancel warp
                if (cfg.partyWarp && CMD_WARP_CANCEL.matcher(msg).matches() && warpDelayUntil > System.currentTimeMillis()) {
                    warpDelayUntil = 0;
                    return;
                }

                // !join <f|m|t><e|0-7> → join instance
                if (cfg.partyJoinInstance && HypixelLocationTracker.getInstance().isInSkyblock() && CMD_JOIN.matcher(msg).matches()) {
                    var jm = CMD_JOIN.matcher(msg);
                    if (jm.find()) {
                        nextCommand = buildJoinCommand(jm.group(1), jm.group(2));
                        runWhenLeader();
                    }
                    return;
                }

                // !sc → send coordinates
                if (cfg.partySendCoords && HypixelLocationTracker.getInstance().isInSkyblock() && CMD_SENDCOORDS.matcher(msg).matches()) {
                    if (self != null) {
                        var pos = self.blockPosition();
                        ChatUtils.sendCommand("pc x: " + pos.getX() + ", y: " + pos.getY() + ", z: " + pos.getZ());
                    }
                }
            });

        // DM party invite (!p in private message)
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().party.dmPartyInvite) return;
            if (!HypixelLocationTracker.getInstance().isOnHypixel()) return;
            var matcher = DM_INVITE.matcher(message.getString());
            if (matcher.find()) {
                String player = ChatUtils.stripColor(matcher.group(1));
                dmInvitePending.put(player.toLowerCase(), System.currentTimeMillis() + 2000);
                ChatUtils.sendCommand("party invite " + player);
                showMsg("party.dm_invited", player);
            }
        });
    }

    private static void runWhenLeader() {
        if (nextCommand == null) return;
        if (!PartyTracker.getInstance().isSelfLeader()) return;
        ChatUtils.sendCommand(nextCommand);
        showMsg("party.executed", "/" + nextCommand);
        nextCommand = null;
    }

    private static String buildJoinCommand(String type, String floor) {
        String prefix = switch (type.toLowerCase()) {
            case "f" -> "CATACOMBS_";
            case "m" -> "MASTER_CATACOMBS_";
            case "t" -> "KUUDRA_";
            default -> "CATACOMBS_";
        };

        if (type.equalsIgnoreCase("t")) {
            String tier = switch (floor.toLowerCase()) {
                case "1" -> "NORMAL"; case "2" -> "HOT"; case "3" -> "BURNING";
                case "4" -> "FIERY"; case "5" -> "INFERNAL";
                default -> "NORMAL";
            };
            return "joininstance " + prefix + tier;
        }

        String level = switch (floor.toLowerCase()) {
            case "e", "0" -> "ENTRANCE"; case "1" -> "FLOOR_ONE"; case "2" -> "FLOOR_TWO";
            case "3" -> "FLOOR_THREE"; case "4" -> "FLOOR_FOUR"; case "5" -> "FLOOR_FIVE";
            case "6" -> "FLOOR_SIX"; case "7" -> "FLOOR_SEVEN";
            default -> "ENTRANCE";
        };
        return "joininstance " + prefix + level;
    }

    private static void showMsg(String key, Object... args) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("babyzombieaddons." + key, args), false);
        }
    }
}
