package top.babyzombie.addons.module.dungeon;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.Minecraft;

public final class DungeonModule {
    private DungeonModule() {}

    public static void init() {
        WelcomeTitle.init();
        BloodReadyAlert.init();
        AutoRequeue.init();
        DupeArcherDetector.init();

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity == Minecraft.getInstance().player) {
                AutoRequeue.inDungeon = false;
            }
        });
    }
}
