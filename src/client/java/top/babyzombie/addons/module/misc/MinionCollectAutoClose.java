package top.babyzombie.addons.module.misc;

import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.ScreenHelper;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.regex.Pattern;

public final class MinionCollectAutoClose {
    private MinionCollectAutoClose() {}

    private static final Pattern MINION_TITLE = Pattern.compile("\\bMinion\\b");

    public static void init() {
        ContainerClickEvents.BEFORE_MOUSE_CLICK.register((screen, slot) -> {
            if (!ModConfigManager.get().general.minionCollectAutoClose) return false;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return false;

            String title = ChatUtils.stripColor(screen.getTitle().getString());
            if (!MINION_TITLE.matcher(title).find()) return false;

            if (slot == null || !slot.hasItem()) return false;

            String name = ChatUtils.stripColor(slot.getItem().getDisplayName().getString()).trim();
            if (!name.equals("Collect All")) return false;

            Scheduler.schedule(1, () -> {
                var client = Minecraft.getInstance();
                if (ScreenHelper.getCurrent() != screen) return;
                client.execute(() -> { if (client.player != null) client.player.closeContainer(); });
            });
            return false;
        });
    }
}
