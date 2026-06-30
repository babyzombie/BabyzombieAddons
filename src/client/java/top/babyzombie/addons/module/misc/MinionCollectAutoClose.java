package top.babyzombie.addons.module.misc;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.regex.Pattern;

public final class MinionCollectAutoClose {
    private MinionCollectAutoClose() {}

    private static final Pattern MINION_TITLE = Pattern.compile("\\bMinion\\b");
    private static final int TIMEOUT_TICKS = 40;
    private static long collectClickTick = -1;

    public static void init() {
        // Detect "Collect All" click — record the tick so the screen-arrival
        // handler can check freshness without a blind timer.
        ContainerClickEvents.BEFORE_MOUSE_CLICK.register((screen, slot, event) -> {
            if (!ModConfigManager.get().general.minionCollectAutoClose) return false;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return false;

            String title = ChatUtils.stripColor(screen.getTitle().getString());
            if (!MINION_TITLE.matcher(title).find()) return false;

            if (slot == null || !slot.hasItem()) return false;

            String name = ChatUtils.stripColor(slot.getItem().getHoverName().getString()).trim();
            if (!name.equals("Collect All")) return false;

            collectClickTick = ServerTick.getTick();
            return false;
        });

        // Hypixel refreshes the minion screen by sending a new OpenScreenPacket.
        // Close it immediately when the refreshed screen arrives — but only if
        // it's soon enough after the click (freshness guard).
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (collectClickTick < 0) return;
            if (ServerTick.getTick() - collectClickTick > TIMEOUT_TICKS) {
                collectClickTick = -1;
                return;
            }
            if (!(screen instanceof AbstractContainerScreen<?> cs)) return;

            String title = ChatUtils.stripColor(cs.getTitle().getString());
            if (!MINION_TITLE.matcher(title).find()) return;

            collectClickTick = -1;
            client.execute(() -> {
                if (client.player != null) client.player.closeContainer();
            });
        });
    }
}
