package top.babyzombie.addons.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import top.babyzombie.addons.util.tracker.HypixelLocationData;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * Fired when the Hypixel Mod API sends a location update packet.
 * Non-cancellable — listeners receive the latest location data for reference.
 */
public final class HypixelLocationEvents {

    public static final Event<LocationUpdate> LOCATION_UPDATE =
            EventFactory.createArrayBacked(LocationUpdate.class, callbacks -> data -> {
                for (LocationUpdate cb : callbacks) {
                    cb.onLocationUpdate(data);
                }
            });

    @FunctionalInterface
    public interface LocationUpdate {
        void onLocationUpdate(HypixelLocationTracker data);
    }

    private HypixelLocationEvents() {}
}
