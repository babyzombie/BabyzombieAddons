package top.babyzombie.addons.module.misc;

import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ScreenLoadWaiter;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.regex.Pattern;

public final class MinionCollectAutoClose {
    private MinionCollectAutoClose() {}

    private static final Pattern MINION_TITLE = Pattern.compile("\\bMinion\\b");
    private static final int TIMEOUT_TICKS = 40;
    private static long collectClickTick = -1;

    public static void init() {
        // Detect "Collect All" click — record the tick for freshness guard
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

        // When the refreshed Minion screen opens and slot 0 loads, close it —
        // but only if soon enough after the click.
        ScreenLoadWaiter.whenScreenOpened(
            title -> MINION_TITLE.matcher(ChatUtils.stripColor(title)).find(),
            0, TIMEOUT_TICKS,
            cs -> {
                if (collectClickTick < 0) return;
                long elapsed = ServerTick.getTick() - collectClickTick;
                collectClickTick = -1;
                if (elapsed > TIMEOUT_TICKS) return;
                var client = Minecraft.getInstance();
                client.execute(() -> {
                    if (client.player != null) client.player.closeContainer();
                });
            });
    }
}
