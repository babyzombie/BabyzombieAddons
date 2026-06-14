package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;

public final class KuudraModule {
    private KuudraModule() {}

    public static void init() {
        KuudraLocationTracker.init();
        KuudraHPDisplay.init();
        KuudraPhaseTimer.init();
        KuudraBoxRenderer.init();
        KuudraEnergyDisplay.init();
        KuudraStunTimer.init();
        KuudraWaypoints.init();
        KuudraPerkShopBlacklist.init();
        EnderPearlRefill.init();
        KuudraFollowerHelmetPrice.init();
        CrimsonArmorPistonMute.init();

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
            if (world != null) KuudraLocationTracker.reset();
        });
    }
}
