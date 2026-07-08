package top.babyzombie.addons.module.chat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.hypixel.data.rank.MonthlyPackageRank;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.tracker.HypixelPlayerInfoTracker;
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

    // DM message: "From PlayerName: !p" or "To PlayerName: !p" (outgoing)
    private static final Pattern DM_INVITE_IN = Pattern.compile(
            "^From (.+): [!！][ ]?p(?:arty)?(?: .*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DM_INVITE_OUT = Pattern.compile(
            "^To (.+): [!！][ ]?p(?:arty)?(?: .*)?$", Pattern.CASE_INSENSITIVE);

    // Party chat commands
    private static final Pattern CMD_ALLINVITE = Pattern.compile("^[!！][ ]?all(?:inv)?(?:ite)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_PINVITE = Pattern.compile("^[!！][ ]?p ([a-zA-Z0-9_-]+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_WARP = Pattern.compile("^[!！][ ]?wa?r?p?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_WARP_CANCEL = Pattern.compile("^[!！][ ]?(?:wa?r?p?)?[ ]?c(?:ancel)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_JOIN = Pattern.compile("^[!！][ ]?(?:join)?[ ]?([fmt])([e0-7])$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_PTME = Pattern.compile("^[!！][ ]?(pt(?:me)?|[叫抢]地主)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_SENDCOORDS = Pattern.compile("^[!！][ ]?(?:s)?(?:end)?[ ]?c(?:oord|oords)?$", Pattern.CASE_INSENSITIVE);
    // !play <content> — execute /play <content>
    private static final Pattern CMD_PLAY = Pattern.compile("^[!！][ ]?play(?: (.+))?$", Pattern.CASE_INSENSITIVE);
    // !stream [number|close] → /stream open|close
    private static final Pattern CMD_STREAM = Pattern.compile("^[!！][ ]?stream(?:[ ]+(\\d+|c(?:lose)?|off))?$", Pattern.CASE_INSENSITIVE);

    /** Strip rank prefix like "[MVP+] " from a player name. */
    private static final Pattern RANK_PREFIX = Pattern.compile("^\\[[\\w+\\+-]+] ");

    // Match "PlayerName has disbanded the party!" / "玩家名解散了组队！"
    private static final Pattern PARTY_DISBAND = Pattern.compile(
            "(.+)( has disbanded the party!|解散了组队！)", Pattern.CASE_INSENSITIVE);

    private static long warpDelayUntil;
    private static String nextCommand;
    private static boolean pendingPlayWarp;
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

        // DM party invite (!p in private message — incoming)
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().party.dmPartyInvite) return;
            if (!HypixelLocationTracker.getInstance().isOnHypixel()) return;
            var matcher = DM_INVITE_IN.matcher(message.getString());
            if (matcher.find()) {
                String raw = ChatUtils.stripColor(matcher.group(1));
                String player = RANK_PREFIX.matcher(raw).replaceFirst("");
                ChatUtils.sendCommand("party invite " + player);
                showMsg("party.dm_invited", player);
            }
        });

        // DM party invite (!p in private message — outgoing)
        // 发出 !p 后，把对方加入 dmInvitePending，等对方邀请时自动接受
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().party.dmPartyInvite) return;
            if (!HypixelLocationTracker.getInstance().isOnHypixel()) return;
            var matcher = DM_INVITE_OUT.matcher(message.getString());
            if (matcher.find()) {
                String raw = ChatUtils.stripColor(matcher.group(1));
                String player = RANK_PREFIX.matcher(raw).replaceFirst("");
                dmInvitePending.put(player.toLowerCase(), ServerTick.getTime() + 2000);
            }
        });

        // !play pit/skyblock 后世界切换完成自动 /p warp
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
            if (!pendingPlayWarp) return;
            pendingPlayWarp = false;
            Scheduler.schedule(60, () -> { // 3 秒延迟等服务器稳定
                var player = Minecraft.getInstance().player;
                if (player == null) return;
                ChatUtils.sendCommand("party warp");
            });
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
        boolean selfSent = self != null && player.equals(self.getName().getString());
        if (selfSent && !cfg.partySelfExecute) return;

        // !pt / !ptme / !叫地主 / !抢地主 → transfer party leader to sender
        if (cfg.partyTransfer && CMD_PTME.matcher(msg).matches()) {
            nextCommand = "party transfer " + player;
            runWhenLeader(selfSent);
            return;
        }

        // !allinv → toggle allinvite
        if (cfg.partyAllinvite && CMD_ALLINVITE.matcher(msg).matches()) {
            nextCommand = "party settings allinvite";
            runWhenLeader(selfSent);
            return;
        }

        // !p <player> → invite
        if (cfg.partyInvite && CMD_PINVITE.matcher(msg).matches()) {
            var pm = CMD_PINVITE.matcher(msg);
            if (pm.find()) {
                nextCommand = "party invite " + pm.group(1);
                runWhenLeader(selfSent);
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
            runWhenLeader(selfSent);
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
                runWhenLeader(selfSent);
            }
            return;
        }

        // !play <content> → /play <content>
        if (cfg.partyPlay && CMD_PLAY.matcher(msg).matches()) {
            var pm = CMD_PLAY.matcher(msg);
            if (pm.find()) {
                String content = pm.group(1);
                nextCommand = "play" + (content != null ? " " + content : "");
                if (content != null && (content.equalsIgnoreCase("pit") || content.equalsIgnoreCase("skyblock"))) {
                    // pit/skyblock 需要队长执行，且切换世界后自动 /p warp
                    var cmd = nextCommand;
                    nextCommand = null;
                    Runnable action = () -> PartyTracker.getInstance().runWhenLeader(() -> {
                        ChatUtils.sendCommand(cmd);
                        showMsg("party.executed", "/" + cmd);
                        pendingPlayWarp = true;
                    });
                    if (selfSent) {
                        scheduleWithDelay(action);
                    } else {
                        action.run();
                    }
                } else {
                    runWhenLeader(selfSent);
                }
            }
            return;
        }

        // !stream → open/close stream (leader + SUPERSTAR only)
        if (cfg.partyStream && CMD_STREAM.matcher(msg).matches()) {
            var info = HypixelPlayerInfoTracker.getInstance().getLastInfo();
            if (info == null || info.monthlyPackageRank() != MonthlyPackageRank.SUPERSTAR) return;
            var sm = CMD_STREAM.matcher(msg);
            if (sm.find()) {
                String arg = sm.group(1);
                if (arg == null) {
                    nextCommand = "stream open";
                } else if (arg.matches("\\d+")) {
                    nextCommand = "stream open " + arg;
                } else {
                    nextCommand = "stream close";
                }
                runWhenLeader(selfSent);
            }
            return;
        }

        // !sc → send coordinates
        if (cfg.partySendCoords && HypixelLocationTracker.getInstance().isInSkyblock() && CMD_SENDCOORDS.matcher(msg).matches()) {
            if (self != null) {
                var pos = self.blockPosition();
                String coordCmd = "pc x: " + pos.getX() + ", y: " + pos.getY() + ", z: " + pos.getZ();
                if (selfSent) {
                    scheduleWithDelay(() -> ChatUtils.sendCommand(coordCmd));
                } else {
                    ChatUtils.sendCommand(coordCmd);
                }
            }
        }
    }

    public static void scheduleAutoAccept(String player) {
        String key = player != null ? player.toLowerCase() : "*";
        pendingAutoAccept.put(key, ServerTick.getTime() + 120000);
    }

    private static void runWhenLeader(boolean selfSent) {
        if (nextCommand == null) return;
        var cmd = nextCommand;
        nextCommand = null;
        Runnable action = () -> PartyTracker.getInstance().runWhenLeader(() -> {
            ChatUtils.sendCommand(cmd);
            showMsg("party.executed", "/" + cmd);
        });
        if (selfSent) {
            scheduleWithDelay(action);
        } else {
            action.run();
        }
    }

    /**
     * 自己发的 party chat 消息，等聊天冷却过去后再执行指令。
     * 延迟 = max(0, 500ms - ping)，确保消息已发出、冷却已重置。
     */
    private static void scheduleWithDelay(Runnable action) {
        int ping = ServerTick.getPing();
        if (ping < 0) ping = 0;
        int delayMs = Math.max(0, 500 - ping);
        int delayTicks = (delayMs + 49) / 50; // 向上取整，避免 <50ms 的延迟被截断为 0
        if (delayTicks > 0) {
            Scheduler.schedule(delayTicks, action);
        } else {
            action.run();
        }
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
