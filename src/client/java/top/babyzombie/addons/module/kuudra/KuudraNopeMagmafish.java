package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

public final class KuudraNopeMagmafish {
    private KuudraNopeMagmafish() {}

    private static final String TARGET_MSG = "[NPC] Elle: Nope, that's just a Magmafish!";
    private static final String TITLE = "Nope, that's just a Magmafish!";
    private static final String SUBTITLE = "               --[NPC] Elle";

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!ModConfigManager.get().kuudra.nopeMagmafish) return;
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return;

            String text = ChatUtils.stripColor(message.getString());
            if (text.equals(TARGET_MSG)) {
                ChatUtils.showTitle(TITLE, SUBTITLE, 0, 60, 40);
            }
        });
    }
}
