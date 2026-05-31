package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.Minecraft;

public final class MiningModule {
    private MiningModule() {}

    public static void init() {
        MiningAbilityAlerts.init();
        ScathaCooldown.init();
        SuspiciousScrapCounter.init();
        CrystalHollowsPassRenew.init();

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity == Minecraft.getInstance().player) {
                MiningAbilityAlerts.readyTime = 0;
                ScathaCooldown.time = 0;
                SuspiciousScrapCounter.count = 0;
            }
        });
    }
}
