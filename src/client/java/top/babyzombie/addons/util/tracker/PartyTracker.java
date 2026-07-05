package top.babyzombie.addons.util.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.hypixel.modapi.HypixelModAPI;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.ServerTick;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;

public final class PartyTracker {

    private static final PartyTracker INSTANCE = new PartyTracker();
    // Match: "The party was transferred to <new> by <old>" or Chinese variants
    private static final Pattern TRANSFER_PAT = Pattern.compile(
            "The party was transferred to (.+) by .+|(.+)已将组队移交给了(.+)|(.+)將組隊移交給了(.+)");
    // Match: "<old> has promoted <new> to Party Leader" or Chinese variants
    private static final Pattern PROMOTE_PAT = Pattern.compile(
            ".+ (has promoted |将|已將)(.+) (to Party Leader|提拔为组队队长|提拔為組隊隊長)");
    // Reset triggers
    private static final Pattern DISBAND_PAT = Pattern.compile(
            "You left the party\\.|你离开了组队。|你離開了組隊。"
                    + "|You have been kicked from the party by .+|你已被.+踢出组队|你已被.+踢出組隊"
                    + "|The party was disbanded because all invites expired and the party was empty\\."
                    + "|因组队中没有成员， 且所有邀请均已过期， 组队已被解散。|因組隊中沒有成員， 且所有邀請均已過期， 組隊已被解散。"
                    + "|.+ has disbanded the party!|.+解散了组队！|.+解散了組隊！");

    private final List<Consumer<PartyInfo>> pendingCallbacks = new ArrayList<>();
    private volatile boolean sendingRequest;
    private final Runnable requestTimeout = () -> { sendingRequest = false; };
    private volatile PartyInfo lastInfo;
    private volatile long lastRequestTime;
    private volatile boolean isLeader;
    private volatile String leaderName;
    private String myName;

    private PartyTracker() {}

    public static PartyTracker getInstance() { return INSTANCE; }

    /** Register Hypixel Mod API packet handler and chat listeners for party tracking. */
    public void init() {
        HypixelModAPI.getInstance().createHandler(ClientboundPartyInfoPacket.class, packet -> {
            var members = packet.getMembers();
            lastInfo = new PartyInfo(members != null ? members : Set.of());
            lastRequestTime = ServerTick.getTime();
            sendingRequest = false;
            Scheduler.cancel(requestTimeout);

            // Try to get leader from member entries
            try {
                var memberMap = packet.getMemberMap();
                if (memberMap != null) {
                    var self = Minecraft.getInstance().player;
                    var selfUUID = self != null ? self.getUUID() : null;
                    for (var member : memberMap.values()) {
                        if (member.getRole() == ClientboundPartyInfoPacket.PartyRole.LEADER) {
                            leaderName = member.getUuid().toString();
                            isLeader = selfUUID != null && selfUUID.equals(member.getUuid());
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}

            List<Consumer<PartyInfo>> callbacks;
            synchronized (pendingCallbacks) {
                callbacks = new ArrayList<>(pendingCallbacks);
                pendingCallbacks.clear();
            }
            for (var cb : callbacks) cb.accept(lastInfo);

            if (ModConfigManager.get().debug.hypixelModApiDebugLog) {
                ChatUtils.showMessage("§7[ModAPI] §dParty §7→ §f" + packet);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (myName == null) {
                var p = Minecraft.getInstance().player;
                if (p != null) myName = p.getName().getString();
            }
            if (DISBAND_PAT.matcher(text).find()) {
                reset();
                return;
            }
            var tm = TRANSFER_PAT.matcher(text);
            if (tm.find()) {
                leaderName = ChatUtils.stripRank(ChatUtils.stripColor(tm.group(1) != null ? tm.group(1)
                        : (tm.group(3) != null ? tm.group(3) : tm.group(5))));
                isLeader = myName != null && myName.equals(leaderName);
                return;
            }
            var pm = PROMOTE_PAT.matcher(text);
            if (pm.find()) {
                leaderName = ChatUtils.stripRank(ChatUtils.stripColor(pm.group(2)));
                isLeader = myName != null && myName.equals(leaderName);
            }
        });
    }

    /** @return true if the local player is the party leader. */
    public boolean isSelfLeader() { return isLeader; }

    /**
     * If we know we're the leader, run immediately.
     * If unsure, queue the action and request party info; action runs when response confirms leadership.
     */
    public void runWhenLeader(Runnable action) {
        if (isLeader) {
            action.run();
            return;
        }
        if (leaderName != null) {
            return;
        }
        synchronized (pendingCallbacks) {
            pendingCallbacks.add(info -> {
                if (isLeader) action.run();
            });
        }
        lastRequestTime = 0;
        request(null);
    }

    /**
     * Run onLeader if we're confirmed as leader, onMember if confirmed as member.
     * If unknown, queue both and request; runs the correct one when response arrives.
     */
    public void runWhenKnown(Runnable onLeader, Runnable onMember) {
        if (isLeader) {
            onLeader.run();
            return;
        }
        if (leaderName != null && myName != null && !myName.equals(leaderName)) {
            onMember.run();
            return;
        }
        // Unknown — request and queue both
        synchronized (pendingCallbacks) {
            pendingCallbacks.add(info -> {
                if (isLeader) onLeader.run(); else onMember.run();
            });
        }
        lastRequestTime = 0;
        request(null);
    }
    /** @return the known party leader's name/UUID. */
    public String getLeaderName() { return leaderName; }

    /** @return true if a party leader is known. */
    public boolean hasLeaderName(String name) {
        // leaderName is now a UUID string; just check if we know the leader
        return leaderName != null;
    }

    /** @return the last fetched party info, or null if never fetched. */
    public PartyInfo getLastInfo() { return lastInfo; }

    /** @return the timestamp (ms) of the last party info fetch, or 0 if never fetched. */
    public long getLastRequestTime() { return lastRequestTime; }

    /** Request party info from the server, with optional callback on response. */
    public void request(Consumer<PartyInfo> callback) {
        if (lastRequestTime + 30_000 > ServerTick.getTime()) {
            if (callback != null) callback.accept(lastInfo);
            return;
        }
        if (callback != null) {
            synchronized (pendingCallbacks) {
                pendingCallbacks.add(callback);
            }
        }
        if (sendingRequest) return;
        HypixelModAPI.getInstance().sendPacket(new ServerboundPartyInfoPacket());
        sendingRequest = true;
        Scheduler.schedule(200, requestTimeout);
    }

    private void reset() {
        lastInfo = null;
        lastRequestTime = 0;
        sendingRequest = false;
        Scheduler.cancel(requestTimeout);
        isLeader = false;
        leaderName = null;
        synchronized (pendingCallbacks) {
            pendingCallbacks.clear();
        }
    }

    public record PartyInfo(Set<UUID> members) {}
}
