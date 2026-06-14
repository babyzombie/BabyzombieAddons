package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;

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

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
            if (world == null) return;
            MiningAbilityAlerts.readyTime = 0;
            ScathaCooldown.time = 0;
            SuspiciousScrapCounter.count = 0;
        });
    }
}
