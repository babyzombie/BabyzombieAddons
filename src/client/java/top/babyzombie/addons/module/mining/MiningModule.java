package top.babyzombie.addons.module.mining;

import top.babyzombie.addons.event.WorldChangeCallback;

public final class MiningModule {
    private MiningModule() {}

    public static void init() {
        MiningAbilityAlerts.init();
        NucleusAutoWarp.init();
        ScathaCooldown.init();
        SuspiciousScrapCounter.init();
        CrystalHollowsPassRenew.init();
        ChGetFromSacks.init();
        ChestMarkers.init();
        ArmadilloEnergy.init();
        GlaciteMineshaftWaypoints.init();

        GreatGlaciteWaypoints.init();
        CreeperVisibility.init();

        PowderMiningSounds.init();
        DrillSwingSuppression.init();

        WorldChangeCallback.register((client, world) -> {
            if (world == null) return;
            MiningAbilityAlerts.readyTime = 0;
            ScathaCooldown.time = 0;
            SuspiciousScrapCounter.count = 0;
        });
    }
}
