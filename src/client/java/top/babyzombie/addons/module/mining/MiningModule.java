package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;

public final class MiningModule {
    private MiningModule() {}

    public static void init() {
        MiningAbilityAlerts.init();
        NucleusAutoWarp.init();
        ScathaCooldown.init();
        SuspiciousScrapCounter.init();
        CrystalHollowsPassRenew.init();
        ChGetFromSacks.init();
        JungleTempleThinWall.init();
        ChestMarkers.init();
        ArmadilloEnergy.init();
        GlaciteMineshaftWaypoints.init();

        GreatGlaciteWaypoints.init();
        CreeperVisibility.init();

        PowderMiningSounds.init();
        DrillSwingSuppression.init();
        MithrilGourmandModule.init();
        CorleoneTimer.init();

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            MiningAbilityAlerts.readyTime = 0;
            ScathaCooldown.time = 0;
            SuspiciousScrapCounter.count = 0;
            JungleTempleThinWall.shown = false;
            CorleoneTimer.reset();
        });
    }
}
