package top.babyzombie.addons.module.party;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.tracker.PartyTracker;
import top.babyzombie.addons.util.ServerTick;

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
            "(?:\\[[\\w+\\+-]+] )?([0-9a-zA-Z_]{2,24})( has invited you to join | has invited all members of .+? to |邀请你加入|已邀请.+?中的所有成员加入)(.+?)( party!|组队！)");

    // Party chat line: "Party > [RANK] PlayerName: message" (5 capture groups matching original JS)
    public static final Pattern PARTY_CHAT = Pattern.compile(
            "(?:Party|组队|組隊) > (?:\\[[^\\]]+\\] )?([0-9a-zA-Z_]{2,24}).*?: (.+)");

    // DM message: "From PlayerName: !p"
    private static final Pattern DM_INVITE = Pattern.compile(
            "^From (.+): [!！][ ]?p(?:arty)?(?: .*)?$", Pattern.CASE_INSENSITIVE);

    // Party chat commands
    private static final Pattern CMD_ALLINVITE = Pattern.compile("^[!！][ ]?all(?:inv)?(?:ite)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_PINVITE = Pattern.compile("^[!！][ ]?p ([a-zA-Z0-9_-]+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_WARP = Pattern.compile("^[!！][ ]?wa?r?p?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_WARP_CANCEL = Pattern.compile("^[!！][ ]?(?:wa?r?p?)?[ ]?c(?:ancel)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_JOIN = Pattern.compile("^[!！][ ]?(?:join)?[ ]?([fmt])([e0-7])$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_PTME = Pattern.compile("^[!！][ ]?pt(?:me)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_SENDCOORDS = Pattern.compile("^[!！][ ]?(?:s)?(?:end)?[ ]?c(?:oord|oords)?$", Pattern.CASE_INSENSITIVE);
    // !play <content> — execute /play <content>
    private static final Pattern CMD_PLAY = Pattern.compile("^[!！][ ]?play(?: (.+))?$", Pattern.CASE_INSENSITIVE);

    /** Strip rank prefix like "[MVP+] " from a player name. */
    private static final Pattern RANK_PREFIX = Pattern.compile("^\\[[\\w+\\+-]+] ");

    // Match "PlayerName has disbanded the party!" / "玩家名解散了组队！"
    private static final Pattern PARTY_DISBAND = Pattern.compile(
            "(.+)( has disbanded the party!|解散了组队！)", Pattern.CASE_INSENSITIVE);

    private static long warpDelayUntil;
    private static String nextCommand;
    private static final Map<String, Long> dmInvitePending = new HashMap<>();
    private static final Map<String, Long> repartyPlayers = new HashMap<>();
    static final Map<String, Long> pendingAutoAccept = new HashMap<>();

    private PartyModule() {}

    public static void init() {

        // Auto-accept party invite
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            var cfg = ModConfigManager.get().party;
            if (!cfg.dmPartyInvite && !cfg.autoAcceptReparty) return;
            String text = message.getString();
            long now = ServerTick.getTime();
            dmInvitePending.values().removeIf(t -> t < now);
            repartyPlayers.values().removeIf(t -> t < now);
            pendingAutoAccept.values().removeIf(t -> t < now);

            var m = PARTY_INVITE.matcher(text);
            if (!m.find()) return;

            String inviter = m.group(1);
            if (inviter == null) return;
            String key = inviter.toLowerCase();

            // DM-triggered auto-accept
            if (cfg.dmPartyInvite && dmInvitePending.containsKey(key)) {
                dmInvitePending.remove(key);
                ChatUtils.sendCommand("party accept " + inviter);
                return;
            }

            // Reparty auto-accept
            if (cfg.autoAcceptReparty && repartyPlayers.containsKey(key)) {
                repartyPlayers.remove(key);
                ChatUtils.sendCommand("party accept " + inviter);
                return;
            }

            // /bza acceptpartyinvite auto-accept
            String matchKey = pendingAutoAccept.containsKey(key) ? key
                    : pendingAutoAccept.containsKey("*") ? "*" : null;
            if (matchKey != null) {
                pendingAutoAccept.remove(matchKey);
                ChatUtils.sendCommand("party accept " + inviter);
            }
        });

        // Detect party disband for reparty tracking
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().party.autoAcceptReparty) return;
            String text = ChatUtils.stripColor(message.getString());
            var m = PARTY_DISBAND.matcher(text);
            if (m.find()) {
                String raw = m.group(1);
                String player = RANK_PREFIX.matcher(raw).replaceFirst("").trim();
                repartyPlayers.put(player.toLowerCase(), ServerTick.getTime() + 120000);
            }
        });

        // Party chat commands
        ClientReceiveMessageEvents.GAME.register(PartyModule::onPartyChat);

        // DM party invite (!p in private message)
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().party.dmPartyInvite) return;
            if (!HypixelLocationTracker.getInstance().isOnHypixel()) return;
            var matcher = DM_INVITE.matcher(message.getString());
            if (matcher.find()) {
                String raw = ChatUtils.stripColor(matcher.group(1));
                String player = RANK_PREFIX.matcher(raw).replaceFirst("");
                dmInvitePending.put(player.toLowerCase(), ServerTick.getTime() + 2000);
                ChatUtils.sendCommand("party invite " + player);
                showMsg("party.dm_invited", player);
            }
        });
    }

    private static void onPartyChat(net.minecraft.network.chat.Component message, boolean overlay) {
        if (overlay) return;
        var cfg = ModConfigManager.get().party;
        String text = ChatUtils.stripColor(message.getString());
        var matcher = PARTY_CHAT.matcher(text);
        if (!matcher.find()) return;

        String player = matcher.group(1);
        String msg = ChatUtils.stripColor(matcher.group(2)).trim();

        var self = Minecraft.getInstance().player;
        if (self != null && player.equals(self.getName().getString()) && !cfg.partySelfExecute) return;

        // !pt / !ptme → transfer party leader to sender
        if (cfg.partyTransfer && CMD_PTME.matcher(msg).matches()) {
            nextCommand = "party transfer " + player;
            runWhenLeader();
            return;
        }

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
                warpDelayUntil = ServerTick.getTime() + cfg.partyWarpDelaySeconds * 1000L;
                return;
            }
            nextCommand = "party warp";
            runWhenLeader();
            return;
        }

        // !c / !warp cancel → cancel warp
        if (cfg.partyWarp && CMD_WARP_CANCEL.matcher(msg).matches() && warpDelayUntil > ServerTick.getTime()) {
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

        // !play <content> → /play <content>
        if (cfg.partyPlay && CMD_PLAY.matcher(msg).matches()) {
            var pm = CMD_PLAY.matcher(msg);
            if (pm.find()) {
                String content = pm.group(1);
                nextCommand = "play" + (content != null ? " " + content : "");
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
    }

    public static void scheduleAutoAccept(String player) {
        String key = player != null ? player.toLowerCase() : "*";
        pendingAutoAccept.put(key, ServerTick.getTime() + 120000);
    }

    private static void runWhenLeader() {
        if (nextCommand == null) return;
        var cmd = nextCommand;
        nextCommand = null;
        PartyTracker.getInstance().runWhenLeader(() -> {
            ChatUtils.sendCommand(cmd);
            showMsg("party.executed", "/" + cmd);
        });
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
            player.sendSystemMessage(
                    Component.translatable("babyzombieaddons." + key, args));
        }
    }
}
