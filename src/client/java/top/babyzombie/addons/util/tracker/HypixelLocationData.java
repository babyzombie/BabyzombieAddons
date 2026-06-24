package top.babyzombie.addons.util.tracker;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public record HypixelLocationData(
    @Nullable String serverName,
    @Nullable String serverType,
    @Nullable String lobbyName,
    @Nullable String mode,
    @Nullable String map,
    @Nullable String uuid,
    @Nullable String profileId,
    @Nullable String location
) {

    /** Construct with minimum fields; remaining fields default to null. */
    public HypixelLocationData(@Nullable String serverName, @Nullable String serverType,
            @Nullable String lobbyName, @Nullable String mode, @Nullable String map,
            @Nullable String uuid, @Nullable String profileId) {
        this(serverName, serverType, lobbyName, mode, map, uuid, profileId, null);
    }

    /** Return a copy of this data with the given UUID. */
    public HypixelLocationData withUuid(String newUuid) {
        return new HypixelLocationData(serverName, serverType, lobbyName, mode, map, newUuid, profileId, location);
    }

    /** Return a copy of this data with the given profile ID. */
    public HypixelLocationData withProfileId(String newProfileId) {
        return new HypixelLocationData(serverName, serverType, lobbyName, mode, map, uuid, newProfileId, location);
    }

    /** Extract the floor number from the location string, e.g. "(F7)" → "F7". */
    @Nullable
    public String getFloor() {
        if (location == null) return null;
        int i = location.lastIndexOf('(');
        int j = location.lastIndexOf(')');
        if (i >= 0 && j > i) return location.substring(i + 1, j);
        return null;
    }

    @Override
    public @NonNull String toString() {
        return String.format(
                "server=%s, type=%s, lobby=%s, mode=%s, map=%s, location=%s, floor=%s, uuid=%s, profile=%s",
                serverName, serverType, lobbyName, mode, map,
                location, getFloor(),
                uuid != null ? uuid.substring(0, Math.min(8, uuid.length())) + "..." : "null",
                profileId != null ? profileId.substring(0, Math.min(8, profileId.length())) + "..." : "null");
    }
}
