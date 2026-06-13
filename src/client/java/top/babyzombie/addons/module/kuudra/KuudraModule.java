package top.babyzombie.addons.module.kuudra;

import top.babyzombie.addons.event.WorldChangeCallback;

public final class KuudraModule {
    private KuudraModule() {}

    public static void init() {
        KuudraLocationTracker.init();
        KuudraHPDisplay.init();
        KuudraPhaseTimer.init();
        KuudraBoxRenderer.init();
        KuudraDirectionIndicator.init();
        KuudraEnergyDisplay.init();
        KuudraStunTimer.init();
        KuudraStunProgress.init();
        KuudraWaypoints.init();
        KuudraPerkShopBlacklist.init();
        EnderPearlRefill.init();
        KuudraFollowerHelmetPrice.init();
        CrimsonArmorPistonMute.init();

        WorldChangeCallback.register((client, world) -> {
            if (world != null) KuudraLocationTracker.reset();
        });
    }
}
