package top.babyzombie.addons.util.tracker;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.hypixel.data.rank.MonthlyPackageRank;
import net.hypixel.data.rank.PackageRank;
import net.hypixel.data.rank.PlayerRank;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPlayerInfoPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPlayerInfoPacket;

/** Tracks the local player's Hypixel rank info via the Hypixel Mod API.
 *  <p>Passively receives {@link ClientboundPlayerInfoPacket} (e.g. when another mod requests it).
 *  Only sends a request on join if no data has been received yet — rank info rarely changes. */
public final class HypixelPlayerInfoTracker {

    private static final HypixelPlayerInfoTracker INSTANCE = new HypixelPlayerInfoTracker();
    private volatile PlayerInfo lastInfo;

    private HypixelPlayerInfoTracker() {}

    public static HypixelPlayerInfoTracker getInstance() { return INSTANCE; }

    /** Register the packet handler and join listener. */
    public void init() {
        // 被动接收：别的模组请求了，我们也能收到
        HypixelModAPI.getInstance().createHandler(ClientboundPlayerInfoPacket.class, packet -> {
            lastInfo = new PlayerInfo(
                    packet.getPlayerRank(),
                    packet.getPackageRank(),
                    packet.getMonthlyPackageRank(),
                    packet.getPrefix().orElse(null)
            );
        });

        // 进服时如果还没收到过数据，主动请求一次
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (lastInfo == null) {
                HypixelModAPI.getInstance().sendPacket(new ServerboundPlayerInfoPacket());
            }
        });

    }

    /** @return the last received player info, or null if none yet. */
    public PlayerInfo getLastInfo() { return lastInfo; }

    public record PlayerInfo(
            PlayerRank playerRank,
            PackageRank packageRank,
            MonthlyPackageRank monthlyPackageRank,
            String prefix
    ) {}
}
