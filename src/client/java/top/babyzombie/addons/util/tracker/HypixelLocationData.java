package top.babyzombie.addons.util.tracker;

import org.jetbrains.annotations.Nullable;

public record HypixelLocationData(
    @Nullable String serverName,
    @Nullable String serverType,
    @Nullable String lobbyName,
    @Nullable String mode,
    @Nullable String map,
    @Nullable String uuid,
    @Nullable String profileId,
    @Nullable String location,
    boolean inSkyblock,
    boolean inDungeon,
    boolean inKuudra,
    boolean inLimbo,
    int skyblockDay
) {

    /** Construct with minimum fields; remaining fields default to null/false/-1. */
    public HypixelLocationData(@Nullable String serverName, @Nullable String serverType,
            @Nullable String lobbyName, @Nullable String mode, @Nullable String map,
            @Nullable String uuid, @Nullable String profileId) {
        this(serverName, serverType, lobbyName, mode, map, uuid, profileId, null, false, false, false, false, -1);
    }

    /** Return a copy of this data with the given UUID. */
    public HypixelLocationData withUuid(String newUuid) {
        return new HypixelLocationData(serverName, serverType, lobbyName, mode, map, newUuid, profileId,
                location, inSkyblock, inDungeon, inKuudra, inLimbo, skyblockDay);
    }

    /** Return a copy of this data with the given profile ID. */
    public HypixelLocationData withProfileId(String newProfileId) {
        return new HypixelLocationData(serverName, serverType, lobbyName, mode, map, uuid, newProfileId,
                location, inSkyblock, inDungeon, inKuudra, inLimbo, skyblockDay);
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
    public String toString() {
        return String.format(
                "server=%s, type=%s, lobby=%s, mode=%s, map=%s, location=%s, floor=%s, inSkyblock=%s, dungeon=%s, kuudra=%s, limbo=%s, day=%d, uuid=%s, profile=%s",
                serverName, serverType, lobbyName, mode, map,
                location, getFloor(),
                inSkyblock,
                inDungeon, inKuudra, inLimbo,
                skyblockDay,
                uuid != null ? uuid.substring(0, Math.min(8, uuid.length())) + "..." : "null",
                profileId != null ? profileId.substring(0, Math.min(8, profileId.length())) + "..." : "null");
    }
}
