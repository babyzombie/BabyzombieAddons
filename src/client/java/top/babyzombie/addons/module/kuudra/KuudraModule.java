package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;

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
        KuudraNopeMagmafish.init();

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            KuudraLocationTracker.reset();
        });
    }
}
