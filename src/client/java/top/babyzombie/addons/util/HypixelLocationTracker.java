package top.babyzombie.addons.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tracks the player's current Hypixel location via the Hypixel Mod API and scoreboard. */
public class HypixelLocationTracker {

    private static final HypixelLocationTracker INSTANCE = new HypixelLocationTracker();
    private static final Pattern PROFILE_ID_PATTERN = Pattern.compile("Profile ID: ([a-f0-9-]+)");
    private static final Pattern LOCATION_PATTERN = Pattern.compile("[⏣ф]");
    private static final int SIDEBAR_SLOT = 1;

    private volatile HypixelLocationData currentLocation;
    private volatile boolean initialized;

    private HypixelLocationTracker() {
        this.currentLocation = new HypixelLocationData(null, null, null, null, null, null, null);
    }

    /** Register Hypixel Mod API packet handler and tick listener. */
    public void init() {
        if (initialized) return;
        initialized = true;

        HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket.class);
        HypixelModAPI.getInstance().createHandler(ClientboundLocationPacket.class, this::onLocationUpdate);

        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);

        // Read scoreboard location every 20 ticks (~1 second)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.player.tickCount % 20 == 0)
                readScoreboard(client);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            currentLocation = new HypixelLocationData(null, null, null, null, null, getUuid(), getProfileId());
        });
    }

    private void onLocationUpdate(ClientboundLocationPacket packet) {
        var uuid = Minecraft.getInstance().getUser().getProfileId().toString();
        String serverType = Objects.requireNonNull(packet.getServerType().orElse(null)).getName();
        String map = packet.getMap().orElse(null);
        boolean inSb = "SkyBlock".equals(serverType);
        var prev = currentLocation;
        currentLocation = new HypixelLocationData(
            packet.getServerName(), serverType,
            packet.getLobbyName().orElse(null), packet.getMode().orElse(null), map,
            uuid, prev.profileId(), prev.location(),
            inSb,
            inSb && "Dungeon".equals(map),
            inSb && "Kuudra".equals(map),
            "limbo".equals(packet.getServerName()),
            prev.skyblockDay()
        );
    }

    private void onGameMessage(Component message, boolean overlay) {
        Matcher matcher = PROFILE_ID_PATTERN.matcher(message.getString());
        if (matcher.find()) {
            String profileId = matcher.group(1);
            var prev = currentLocation;
            String uuid = prev.uuid();
            if (uuid == null) uuid = Minecraft.getInstance().getUser().getProfileId().toString();
            currentLocation = new HypixelLocationData(
                    prev.serverName(), prev.serverType(),
                    prev.lobbyName(), prev.mode(), prev.map(),
                    uuid, profileId, prev.location(),
                    prev.inSkyblock(),
                    prev.inDungeon(), prev.inKuudra(),
                    prev.inLimbo(), prev.skyblockDay());
        }
    }

    private void readScoreboard(Minecraft client) {
        ClientLevel world = client.level;
        if (world == null || !isInSkyblock()) return;

        Scoreboard sb = world.getScoreboard();
        Objective obj = sb.getDisplayObjective(DisplaySlot.BY_ID.apply(SIDEBAR_SLOT));
        if (obj == null) return;

        String newLocation = null;
        for (ScoreHolder holder : sb.getTrackedPlayers()) {
            if (!sb.listPlayerScores(holder).containsKey(obj)) continue;
            PlayerTeam team = sb.getPlayersTeam(holder.getScoreboardName());
            if (team == null) continue;
            String text = team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString();
            String plain = ChatFormatting.stripFormatting(text).trim();
            if (!plain.isEmpty() && LOCATION_PATTERN.matcher(plain).find()) {
                newLocation = ChatUtils.removeEmoji(
                        ChatUtils.stripColor(plain).replaceAll("[⏣ф]", "").trim());
                break;
            }
        }

        // Fallback: if scoreboard has no location marker (e.g. Private Island), use the API map
        if (newLocation == null) {
            String map = currentLocation.map();
            if (map != null && !map.isEmpty()) {
                newLocation = map;
            }
        }

        if (newLocation != null) {
            var prev = currentLocation;
            int day = (int)(world.getDayTime() / 24000L);
            currentLocation = new HypixelLocationData(
                    prev.serverName(), prev.serverType(),
                    prev.lobbyName(), prev.mode(), prev.map(),
                    prev.uuid(), prev.profileId(), newLocation,
                    prev.inSkyblock(),
                    prev.inDungeon(), prev.inKuudra(),
                    prev.inLimbo(), day);
        }
    }

    // ---- getters ----

    public static HypixelLocationTracker getInstance() { return INSTANCE; }
    /** @return the latest known location snapshot. */
    public HypixelLocationData getCurrentLocation() { return currentLocation; }

    @Nullable public String getServerName() { return currentLocation.serverName(); }
    @Nullable public String getServerType() { return currentLocation.serverType(); }
    @Nullable public String getLobbyName() { return currentLocation.lobbyName(); }
    @Nullable public String getMode() { return currentLocation.mode(); }
    @Nullable public String getMap() { return currentLocation.map(); }
    public boolean isOnHypixel() { return currentLocation.serverName() != null; }
    public boolean isInSkyblock() { return currentLocation.inSkyblock(); }
    @Nullable public String getUuid() { return currentLocation.uuid(); }
    @Nullable public String getProfileId() { return currentLocation.profileId(); }
    @Nullable public String getLocation() { return currentLocation.location(); }
    public boolean isInDungeon() { return currentLocation.inDungeon(); }
    public boolean isInKuudra() { return currentLocation.inKuudra(); }
    public boolean isInLimbo() { return currentLocation.inLimbo(); }
    public int getSkyblockDay() { return currentLocation.skyblockDay(); }
    /** @return the dungeon floor from the location string, or null. */
    @Nullable public String getFloor() { return currentLocation.getFloor(); }
}
