package top.babyzombie.addons.util;

import org.jetbrains.annotations.Nullable;

public record HypixelLocationData(
    @Nullable String serverName,
    @Nullable String serverType,
    @Nullable String lobbyName,
    @Nullable String mode,
    @Nullable String map,
    @Nullable String uuid,
    @Nullable String profileId
) {

    public HypixelLocationData withUuid(String newUuid) {
        return new HypixelLocationData(serverName, serverType, lobbyName, mode, map, newUuid, profileId);
    }

    public HypixelLocationData withProfileId(String newProfileId) {
        return new HypixelLocationData(serverName, serverType, lobbyName, mode, map, uuid, newProfileId);
    }

    @Override
    public String toString() {
        return "HypixelLocationData{" +
                "serverName='" + serverName + '\'' +
                ", serverType='" + serverType + '\'' +
                ", lobbyName='" + lobbyName + '\'' +
                ", mode='" + mode + '\'' +
                ", map='" + map + '\'' +
                ", uuid='" + uuid + '\'' +
                ", profileId='" + profileId + '\'' +
                '}';
    }
}
