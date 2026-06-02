package top.babyzombie.addons.module.dungeon;

import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.PartyTracker;
import top.babyzombie.addons.util.Scheduler;

public final class AutoRequeue {
    static boolean cancelAutoJoin;
    static boolean ended;
    private static boolean counting;
    private static boolean canRequeue;

    private AutoRequeue() {}

    static void init() {}

    static void onInstanceStart() {
        cancelAutoJoin = false;
        ended = false;
        PartyTracker.getInstance().request(info -> {
            var members = info.members();
            var player = Minecraft.getInstance().player;
            canRequeue = members.isEmpty()
                    || (player != null && members.contains(player.getUUID()));
        });
    }

    static void schedule(boolean win) {
        var cfg = ModConfigManager.get().dungeon;
        ModConfig.RequeueMode mode = cfg.autoRequeue;
        if (mode == ModConfig.RequeueMode.OFF) return;
        if (ended) return;
        if (mode == ModConfig.RequeueMode.ON_FAIL && win) return;
        if (mode == ModConfig.RequeueMode.ON_WIN && !win) return;

        if (!canRequeue) return;

        ended = true;
        counting = true;
        int delay = cfg.requeueDelay;
        if (!cfg.requeueMessage.isEmpty()) {
            String msg = cfg.requeueMessage.replace("%delay%", String.valueOf(delay));
            ChatUtils.sendCommand("pc " + msg);
        }

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
        if (!cancelMsg.isEmpty()) ChatUtils.sendCommand("pc " + cancelMsg);
    }
}
