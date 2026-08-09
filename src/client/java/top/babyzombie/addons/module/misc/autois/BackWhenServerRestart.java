package top.babyzombie.addons.module.misc.autois;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.HypixelLocationEvents;
import top.babyzombie.addons.mixin.window.FramerateLimitTrackerAccessor;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.regex.Pattern;

public class BackWhenServerRestart {
    /** 服务器重启时迁移到 hub 的提示消息(全文匹配) */
    private static final Pattern EVACUATE_MSG = Pattern.compile(
        "Evacuating to Hub\\.\\.\\."
    );
    /** 挂机阈值:连续 30 秒无键盘/鼠标输入 */
    private static final long AFK_MS = 30_000;
    /** 落到 hub 后等待 10 秒再发 /is,给私人岛服务器恢复时间 */
    private static final int RETURN_DELAY_TICKS = 200;
    /** 收到迁移消息且确认挂机,等待下一次位置更新后回岛 */
    private static boolean waitingReturn = false;

    private BackWhenServerRestart() {}

    /**
     * 距最近一次键盘/鼠标输入过去的毫秒数。直接读 MC 自带的
     * {@code FramerateLimitTracker.latestInputTime}(输入到达时 MC 更新,走墙上时钟),
     * 不用 ServerTick(TPS 调整/断线时失真),不依赖 GLFW(项目在去 GLFW 化)。
     */
    private static long idleMs() {
        var tracker = Minecraft.getInstance().getFramerateLimitTracker();
        return Util.getMillis() - ((FramerateLimitTrackerAccessor) tracker).getLatestInputTime();
    }

    static void init() {
        ClientReceiveMessageEvents.GAME.register((component, o) -> {
            if (ModConfigManager.get().skyblock.autois.enabled) return;
            if (!ModConfigManager.get().skyblock.autois.backOnServerRestart) return;
            // 全文匹配迁移提示,且确认玩家已挂机 30 秒(有输入就说明人在,自己会处理)
            if (EVACUATE_MSG.matcher(component.getString()).matches()
                && idleMs() >= AFK_MS) {
                waitingReturn = true;
            }
        });

        // 玩家被迁移到 hub 时 ModAPI 会推送新的 location 包
        HypixelLocationEvents.LOCATION_UPDATE.register(data -> {
            if (!waitingReturn) return;
            waitingReturn = false;
            // 位置已更新(玩家落到 hub),挂 10 秒计时器后回岛
            if (data.isIn("Hub")) Scheduler.schedule(RETURN_DELAY_TICKS, () -> {
                var tracker = HypixelLocationTracker.getInstance();
                if (tracker.isInSkyblock() && !tracker.isIn("Private Island")) {
                    ChatUtils.sendCommand("is");
                }
            });
        });
    }
}
