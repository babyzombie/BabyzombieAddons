package top.babyzombie.addons.module.dungeon;

import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.tracker.PartyTracker;
import top.babyzombie.addons.util.Scheduler;

public final class AutoRequeue {
    static boolean cancelAutoJoin;
    static boolean ended;
    static boolean canRequeue;
    static boolean waitingForRevive;
    static boolean reviveCheckRegistered;

    private AutoRequeue() {}

    private static final Runnable REVIVE_CHECK = new Runnable() {
        @Override
        public void run() {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            if (cancelAutoJoin || !waitingForRevive) {
                reviveCheckRegistered = false;
                Scheduler.cancel(this);
                return;
            }
            if (!player.isInvisible()) {
                waitingForRevive = false;
                reviveCheckRegistered = false;
                Scheduler.cancel(this);
                tryRequeue();
            }
        }
    };

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

    static void tryRequeue() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (cancelAutoJoin) return;
        if (PartyTracker.getInstance().getLeaderName() != null && !PartyTracker.getInstance().isSelfLeader()) return;
        var loc = HypixelLocationTracker.getInstance();
        if (!loc.isInSkyblock() || !(loc.isInDungeon() || loc.isInKuudra())) return;

        if (player.isInvisible()) {
            if (!reviveCheckRegistered) {
                waitingForRevive = true;
                reviveCheckRegistered = true;
                Scheduler.scheduleRepeating(1, REVIVE_CHECK);
            }
            return;
        }

        waitingForRevive = false;
        ended = false;
        ChatUtils.sendCommand("instancerequeue");
    }

    static void schedule(boolean win) {
        var cfg = ModConfigManager.get().dungeon;
        var t = HypixelLocationTracker.getInstance();
        boolean isKuudra = t.isInKuudra();
        ModConfig.RequeueMode mode = isKuudra ? ModConfigManager.get().kuudra.requeue.kuudraRequeue : cfg.requeue.dungeonRequeue;
        if (mode == ModConfig.RequeueMode.OFF) return;
        if (ended) return;
        if (mode == ModConfig.RequeueMode.ON_FAIL && win) return;
        if (mode == ModConfig.RequeueMode.ON_WIN && !win) return;

        if (!canRequeue || cancelAutoJoin) return;

        ended = true;
        int delay = isKuudra ? ModConfigManager.get().kuudra.requeue.kuudraRequeueDelay : cfg.requeue.dungeonRequeueDelay;
        if (!cfg.requeue.requeueMessage.isEmpty() && PartyTracker.getInstance().isSelfLeader()) {
            String msg = cfg.requeue.requeueMessage.replace("%delay%", String.valueOf(delay));
            ChatUtils.sendCommand("pc " + msg);
        }

        if (delay > 0) {
            Scheduler.schedule(delay * 20, AutoRequeue::tryRequeue);
        } else {
            tryRequeue();
        }
    }

    static void cancel() {
        cancelAutoJoin = true;
        waitingForRevive = false;
        String cancelMsg = ModConfigManager.get().dungeon.requeue.requeueCancelMessage;
        if (!cancelMsg.isEmpty()) ChatUtils.sendCommand("pc " + cancelMsg);
    }
}
