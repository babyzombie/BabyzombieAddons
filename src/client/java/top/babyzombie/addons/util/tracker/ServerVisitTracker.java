package top.babyzombie.addons.util.tracker;

import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.HypixelLocationEvents;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Records every server name the player has visited during this session,
 * with automatic expiration based on {@code MiscConfig.serverVisitExpireMinutes}.
 * Listens to {@link HypixelLocationEvents#LOCATION_UPDATE} to passively log servers.
 */
public final class ServerVisitTracker {

    private static final ServerVisitTracker INSTANCE = new ServerVisitTracker();

    private final Map<String, Long> visitedServers = new LinkedHashMap<>();
    private String lastRecordedServer;

    private ServerVisitTracker() {}

    public static ServerVisitTracker getInstance() { return INSTANCE; }

    /** Register the location-listener that records visited server names. */
    public void init() {
        HypixelLocationEvents.LOCATION_UPDATE.register(this::onLocationUpdate);
    }

    private void onLocationUpdate(HypixelLocationData data) {
        String serverName = data.serverName();
        if (serverName != null && !serverName.equals(lastRecordedServer)) {
            visitedServers.put(serverName, System.currentTimeMillis());
            lastRecordedServer = serverName;
        }
    }

    /**
     * Check whether a server has been visited and the record has NOT expired.
     * Expired entries are cleaned up lazily during this call.
     */
    public boolean hasVisited(String serverName) {
        purgeExpired();
        return visitedServers.containsKey(serverName);
    }

    /** Return an unmodifiable snapshot of currently active (non-expired) server names. */
    public Set<String> getVisitedServers() {
        purgeExpired();
        return Collections.unmodifiableSet(visitedServers.keySet());
    }

    public void clear() {
        visitedServers.clear();
        lastRecordedServer = null;
    }

    private void purgeExpired() {
        int expireMinutes = ModConfigManager.get().misc.serverVisitExpireMinutes;
        if (expireMinutes <= 0) return;
        long cutoff = System.currentTimeMillis() - expireMinutes * 60_000L;
        visitedServers.values().removeIf(recordedAt -> recordedAt < cutoff);
    }
}
