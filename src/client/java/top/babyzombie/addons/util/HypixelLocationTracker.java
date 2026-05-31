package top.babyzombie.addons.util;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HypixelLocationTracker {

    private static final HypixelLocationTracker INSTANCE = new HypixelLocationTracker();
    private static final Pattern PROFILE_ID_PATTERN = Pattern.compile("Profile ID: ([a-f0-9-]+)");

    private volatile HypixelLocationData currentLocation;
    private volatile boolean initialized;

    private HypixelLocationTracker() {
        this.currentLocation = new HypixelLocationData(null, null, null, null, null, null, null);
    }

    /**
     * Called once during mod initialization. Subscribes to HypixelModAPI
     * location events and begins tracking the player's server-side location.
     */
    public void init() {
        if (initialized) return;
        initialized = true;

        HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket.class);
        HypixelModAPI.getInstance().createHandler(ClientboundLocationPacket.class, this::onLocationUpdate);

        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            currentLocation = new HypixelLocationData(null, null, null, null, null, getUuid(), getProfileId());
        });
    }

    private void onLocationUpdate(ClientboundLocationPacket packet) {
        var uuid = Minecraft.getInstance().getUser().getProfileId().toString();
        currentLocation = new HypixelLocationData(
            packet.getServerName(),
            Objects.requireNonNull(packet.getServerType().orElse(null)).getName(),
            packet.getLobbyName().orElse(null),
            packet.getMode().orElse(null),
            packet.getMap().orElse(null),
            uuid,
            currentLocation.profileId()
        );
    }

    private void onGameMessage(Component message, boolean overlay) {
        Matcher matcher = PROFILE_ID_PATTERN.matcher(message.getString());
        if (matcher.find()) {
            String profileId = matcher.group(1);
            String uuid = currentLocation.uuid();
            if (uuid == null) {
                uuid = Minecraft.getInstance().getUser().getProfileId().toString();
            }
            currentLocation = currentLocation.withUuid(uuid).withProfileId(profileId);
        }
    }

    // ---- getters ----

    public static HypixelLocationTracker getInstance() {
        return INSTANCE;
    }

    public HypixelLocationData getCurrentLocation() {
        return currentLocation;
    }

    @Nullable
    public String getServerName() {
        return currentLocation.serverName();
    }

    @Nullable
    public String getServerType() {
        return currentLocation.serverType();
    }

    @Nullable
    public String getLobbyName() {
        return currentLocation.lobbyName();
    }

    @Nullable
    public String getMode() {
        return currentLocation.mode();
    }

    @Nullable
    public String getMap() {
        return currentLocation.map();
    }

    public boolean isOnHypixel() {
        return currentLocation.serverName() != null;
    }

    public boolean isInSkyblock() {
        return "SkyBlock".equals(currentLocation.serverType());
    }

    @Nullable
    public String getUuid() {
        return currentLocation.uuid();
    }

    @Nullable
    public String getProfileId() {
        return currentLocation.profileId();
    }
}
