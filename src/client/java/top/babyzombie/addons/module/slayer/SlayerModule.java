package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.Minecraft;

public final class SlayerModule {
    private SlayerModule() {}

    public static void init() {
        BossDetector.init();
        PigmanSwordTimer.init();
        RagnarockAxeTimer.init();
        ReaperArmorTimer.init();
        EndStoneSwordTimer.init();
        SlayerHUD.init();

        // Reset on world load
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity == Minecraft.getInstance().player) {
                BossDetector.currentBoss = null;
                BossDetector.bossType = "";
                BossDetector.bossHP = 0;
                BossDetector.bossMaxHP = 0;
                PigmanSwordTimer.time = 0;
                RagnarockAxeTimer.castTime = 0;
                RagnarockAxeTimer.duration = 0;
                RagnarockAxeTimer.cooldown = 0;
                ReaperArmorTimer.time = 0;
                EndStoneSwordTimer.time = 0;
                EndStoneSwordTimer.active = false;
            }
        });
    }
}
