package top.babyzombie.addons.module.kuudra;

public final class KuudraModule {
    private KuudraModule() {}

    public static void init() {
        ArrowPoisonRefill.init();
        KuudraLocationTracker.init();
        KuudraScreenProtector.init();
        KuudraHPDisplay.init();
        KuudraPhaseTimer.init();
        KuudraBoxRenderer.init();
        KuudraEnergyDisplay.init();
        KuudraStunTimer.init();
        KuudraWaypoints.init();
        EnderPearlTrajectory.init();
        EnderPearlCamera.init();
        KuudraPerkShopBlacklist.init();
        EnderPearlRefill.init();
        KuudraFollowerHelmetPrice.init();
        CrimsonArmorPistonMute.init();
        KuudraNopeMagmafish.init();
        KuudraSupplyTimer.init();
        KuudraSupplyProgressHUD.init();
        KuudraPileWaypoints.init();
        PearlWaypoints.init();
        FreshSystem.init();
        NoPreAlert.init();
        AlreadyPickingAlert.init();
        ElleHighlight.init();
        TeamHighlight.init();
        KuudraDirectionHUD.init();
        KuudraP4Features.init();
        ChestCounter.init();
        KuudraEtherwarpLavaPrevent.init();
    }
}
