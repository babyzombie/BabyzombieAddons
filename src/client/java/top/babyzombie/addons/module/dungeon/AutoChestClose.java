package top.babyzombie.addons.module.dungeon;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.Scheduler;

/**
 * Auto-closes chest GUIs shortly after opening them in dungeons.
 */
public final class AutoChestClose {

    private AutoChestClose() {}

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (!ModConfigManager.get().dungeon.autoChestClose) return;
            if (!HypixelLocationTracker.getInstance().isInDungeon()) return;
            if (screen instanceof AbstractContainerScreen<?> cs) {
                String title = cs.getTitle().getString();
                if (title.equals("Chest") || title.equals("箱子")) {
                    Scheduler.schedule(4, () -> {
                        if (client.screen == screen && HypixelLocationTracker.getInstance().isInDungeon())
                            client.execute(() -> { if (client.player != null) client.player.closeContainer(); });
                    });
                }
            }
        });
    }
}
