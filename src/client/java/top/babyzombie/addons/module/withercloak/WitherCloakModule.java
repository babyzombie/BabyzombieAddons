package top.babyzombie.addons.module.withercloak;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;

public final class WitherCloakModule {
    private WitherCloakModule() {}

    public static void init() {
        WitherCloakTimer.init();
        SoulwardTimer.init();
        AlignedTimer.init();
        GravityStormTimer.init();
        ChargedCreeperHider.init();
        WitherCloakHUD.init();

        // Reset on world load
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            if (world == null) return;
            WitherCloakTimer.duration = 0;
            WitherCloakTimer.cooldown = 0;
            WitherCloakTimer.active = false;
            SoulwardTimer.duration = 0;
            SoulwardTimer.cooldown = 0;
            AlignedTimer.time = 0;
            AlignedTimer.by = "";
            GravityStormTimer.time = 0;
        });
    }
}
