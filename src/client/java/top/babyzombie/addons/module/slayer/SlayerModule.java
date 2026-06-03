package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;

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
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
            if (world == null) return;
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
        });
    }
}
