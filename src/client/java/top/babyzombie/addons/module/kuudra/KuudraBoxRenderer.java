package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.GlowController;

public final class KuudraBoxRenderer {
    private KuudraBoxRenderer() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var e = KuudraLocationTracker.kuudraEntity;
            if (e == null || e.isDeadOrDying()) return;

            boolean wantGlow = ModConfigManager.get().kuudra.boxKuudra && KuudraLocationTracker.hp > 1;
            GlowController.setGlow(e, wantGlow, 0xF7510F);
        });
    }
}
