package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.Minecraft;

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

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity == Minecraft.getInstance().player)
                KuudraLocationTracker.reset();
        });
    }
}
