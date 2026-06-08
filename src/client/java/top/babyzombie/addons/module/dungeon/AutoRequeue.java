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
    static boolean canRequeue;

    private AutoRequeue() {}

    static void init() {}

    static void onInstanceStart() {
        cancelAutoJoin = false;
        ended = false;
        PartyTracker.getInstance().request(info -> {
            var members = info.members();
            var player = Minecraft.getInstance().player;
            canRequeue = members.isEmpty()
                    || (player != null && PartyTracker.getInstance().isSelfLeader());
        });
    }

    static void schedule(boolean win) {
        var cfg = ModConfigManager.get().dungeon;
        var t = HypixelLocationTracker.getInstance();
        boolean isKuudra = t.isInKuudra();
        ModConfig.RequeueMode mode = isKuudra ? cfg.kuudraRequeue : cfg.dungeonRequeue;
        if (mode == ModConfig.RequeueMode.OFF) return;
        if (ended) return;
        if (mode == ModConfig.RequeueMode.ON_FAIL && win) return;
        if (mode == ModConfig.RequeueMode.ON_WIN && !win) return;

        if (!canRequeue) return;

        ended = true;
        int delay = isKuudra ? cfg.kuudraRequeueDelay : cfg.dungeonRequeueDelay;
        if (!cfg.requeueMessage.isEmpty()) {
            String msg = cfg.requeueMessage.replace("%delay%", String.valueOf(delay));
            ChatUtils.sendCommand("pc " + msg);
        }

        if (delay > 0) {
            Scheduler.schedule(delay * 20, () -> {
                if (cancelAutoJoin) return;
                if (!PartyTracker.getInstance().isSelfLeader()) return;
                var loc = HypixelLocationTracker.getInstance();
                if (!loc.isInSkyblock() || (!loc.isInDungeon() && !loc.isInKuudra())) return;
                ended = false;
                ChatUtils.sendCommand("instancerequeue");
            });
        } else {
            if (!PartyTracker.getInstance().isSelfLeader()) return;
            ended = false;
            ChatUtils.sendCommand("instancerequeue");
        }
    }

    static void cancel() {
        cancelAutoJoin = true;
        String cancelMsg = ModConfigManager.get().dungeon.requeueCancelMessage;
        if (!cancelMsg.isEmpty()) ChatUtils.sendCommand("pc " + cancelMsg);
    }
}
