package top.babyzombie.addons.module.dungeon;

import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.Scheduler;

/**
 * Auto requeue after dungeon/kuudra end with configurable delay and mode.
 */
public final class AutoRequeue {
    static boolean cancelAutoJoin;
    static boolean ended;
    private static boolean counting;

    private AutoRequeue() {}

    static void init() {}

    static void schedule(boolean win) {
        var cfg = ModConfigManager.get().dungeon;
        ModConfig.RequeueMode mode = cfg.autoRequeue;
        if (mode == ModConfig.RequeueMode.OFF) return;
        if (ended) return;
        if (mode == ModConfig.RequeueMode.ON_FAIL && win) return;
        if (mode == ModConfig.RequeueMode.ON_WIN && !win) return;

        ended = true;
        counting = true;
        int delay = cfg.requeueDelay;
        String msg = cfg.requeueMessage.replace("%delay%", String.valueOf(delay));
        ChatUtils.sendCommand("pc " + msg);

        if (delay > 0) {
            Scheduler.schedule(delay * 20, () -> {
                counting = false;
                if (cancelAutoJoin) return;
                var t = HypixelLocationTracker.getInstance();
                if (!t.isInSkyblock() || (!t.isInDungeon() && !t.isInKuudra())) return;
                ended = false;
                ChatUtils.sendCommand("instancerequeue");
            });
        } else {
            counting = false;
            ended = false;
            ChatUtils.sendCommand("instancerequeue");
        }
    }

    static void cancel() {
        if (!ended && !counting) return;
        cancelAutoJoin = true;
        String cancelMsg = ModConfigManager.get().dungeon.requeueCancelMessage;
        ChatUtils.sendCommand("pc " + cancelMsg);
    }
}
