package top.babyzombie.addons.module.dungeon;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
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
                if (cs.getTitle().getContents() instanceof TranslatableContents tc
                        && ("container.chest".equals(tc.getKey()) || "container.chestDouble".equals(tc.getKey()))) {
                    Scheduler.schedule(4, () -> {
                        if (client.screen == screen && HypixelLocationTracker.getInstance().isInDungeon())
                            client.execute(() -> { if (client.player != null) client.player.closeContainer(); });
                    });
                }
            }
        });
    }
}
