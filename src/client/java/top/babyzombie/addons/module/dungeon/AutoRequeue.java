package top.babyzombie.addons.module.dungeon;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
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

    private static final String SPECTATOR_ERROR = "You are not allowed to use that command as a spectator!";

    private AutoRequeue() {}

    static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoRequeue::tick);

        // 死亡瞬间发 instancerequeue 可能被服务器以 spectator 为由拒绝,此时启动复活检测,等复活后重发
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || cancelAutoJoin || !canRequeue) return;
            if (ChatUtils.stripColor(message.getString()).equals(SPECTATOR_ERROR)) {
                waitingForRevive = true;
            }
        });
    }

    /** 每 tick 检查玩家是否复活。绝大多数时候 waitingForRevive 为 false,直接 return。 */
    private static void tick(Minecraft client) {
        if (!waitingForRevive) return;
        var player = client.player;
        if (player == null) return;
        if (cancelAutoJoin) {
            waitingForRevive = false;
            return;
        }
        if (!player.isInvisible()) {
            waitingForRevive = false;
            tryRequeue();
        }
    }

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
        if (requeueMode(loc.isInKuudra()) == ModConfig.RequeueMode.OFF) return;

        if (player.isInvisible()) {
            waitingForRevive = true;
            return;
        }

        waitingForRevive = false;
        ended = false;
        ChatUtils.sendCommand("instancerequeue");
    }

    private static ModConfig.RequeueMode requeueMode(boolean isKuudra) {
        return isKuudra
                ? ModConfigManager.get().kuudra.requeue.kuudraRequeue
                : ModConfigManager.get().dungeon.requeue.dungeonRequeue;
    }

    static void schedule(boolean win) {
        var cfg = ModConfigManager.get().dungeon;
        var t = HypixelLocationTracker.getInstance();
        boolean isKuudra = t.isInKuudra();
        ModConfig.RequeueMode mode = requeueMode(isKuudra);
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
