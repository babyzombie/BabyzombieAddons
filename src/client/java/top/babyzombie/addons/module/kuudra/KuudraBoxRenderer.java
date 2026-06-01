package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import top.babyzombie.addons.config.ModConfigManager;

public final class KuudraBoxRenderer {
    private KuudraBoxRenderer() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var e = KuudraLocationTracker.kuudraEntity;
            if (e == null || e.isDeadOrDying()) return;

            boolean wantGlow = ModConfigManager.get().kuudra.boxKuudra && KuudraLocationTracker.hp > 1;
            boolean hasGlow = e.hasEffect(MobEffects.GLOWING);

            if (wantGlow && !hasGlow)
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE, 0, true, false));
            else if (!wantGlow && hasGlow)
                e.removeEffect(MobEffects.GLOWING);
        });
    }
}
