package top.babyzombie.addons.module.misc;

import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.regex.Pattern;

public final class MinionCollectAutoClose {
    private MinionCollectAutoClose() {}

    private static final Pattern MINION_TITLE = Pattern.compile("\\bMinion\\b");

    public static void init() {
        ContainerClickEvents.BEFORE_MOUSE_CLICK.register((screen, slot, button) -> {
            if (!ModConfigManager.get().general.minionCollectAutoClose) return false;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return false;

            String title = ChatUtils.stripColor(screen.getTitle().getString());
            if (!MINION_TITLE.matcher(title).find()) return false;

            if (slot == null || !slot.hasItem()) return false;

            String name = ChatUtils.stripColor(slot.getItem().getHoverName().getString()).trim();
            if (!name.equals("Collect All")) return false;

            Scheduler.schedule(ServerTick.getPing() / 50 + 4, () -> {
                var client = Minecraft.getInstance();
                var cur = client.screen;
                if (cur == null) return;
                String curTitle = ChatUtils.stripColor(cur.getTitle().getString());
                if (!MINION_TITLE.matcher(curTitle).find()) return;
                client.execute(() -> { if (client.player != null) client.player.closeContainer(); });
            });
            return false;
        });
    }
}
