package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.util.ChatUtils;

public final class KuudraModule {
    private KuudraModule() {}

    public static void init() {
        KuudraWelcomeTitle.init();
        KuudraHPDisplay.init();
        KuudraPhaseTimer.init();
        EnderPearlRefill.init();

        // Track Kuudra location
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("Kuudra") && (text.contains("T1") || text.contains("T2") || text.contains("T3")
                || text.contains("T4") || text.contains("T5"))) {
                KuudraHPDisplay.inKuudra = true;
            }
        });

        // Reset on world load
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity == Minecraft.getInstance().player) {
                KuudraHPDisplay.inKuudra = false;
                KuudraHPDisplay.kuudraEntity = null;
                KuudraPhaseTimer.currentPhase = "";
            }
        });
    }
}
