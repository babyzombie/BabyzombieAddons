package top.babyzombie.addons.module.misc;

import com.sun.management.OperatingSystemMXBean;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.mixin.window.FramerateLimitTrackerAccessor;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.ServerTickCounter;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WindowTitleModule {

    /// 缓存 createTitle() 返回的原始窗口标题
    public static String cachedOriginalTitle;

    private WindowTitleModule() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(WindowTitleModule::onTick);
    }

    private static void onTick(Minecraft client) {
        var wt = ModConfigManager.get().general;
        if (!wt.windowTitle.enabled) return;
        if (client.player == null) return;

        int interval = Math.clamp(wt.windowTitle.updateInterval, 1, 20);
        if (client.player.tickCount % interval != 0) return;

        String title = buildTitle(cachedOriginalTitle);
        if (title != null && client.getWindow() != null) {
            client.getWindow().setTitle(title);
        }
    }

    /// 拼装窗口标题，返回 null 表示不要修改
    public static String buildTitle(String originalTitle) {
        var wt = ModConfigManager.get().general;
        if (!wt.windowTitle.enabled) return null;

        StringBuilder sb = new StringBuilder();

        HypixelLocationTracker tracker = HypixelLocationTracker.getInstance();

        // ── 前缀 ──
        if (wt.windowTitle.overrideOriginal && wt.windowTitle.showLocation && tracker.isOnHypixel()) {
            // 覆盖模式仅在位置显示开启时生效：在 Hypixel 用 "Hypixel" 顶替原前缀
            sb.append("Hypixel");
        } else {
            if (originalTitle != null && !originalTitle.isEmpty()) {
                sb.append(originalTitle);
            }
        }

        // ── 自定义片段（位置 → 世界天数 → 内存 → 系统内存 → 延迟 → 会话 → 挂机）──
        List<String> parts = new ArrayList<>();

        if (wt.windowTitle.showLocation) {
            String loc = buildLocationString(tracker);
            if (loc != null) {
                parts.add(loc);
            }
        }

        if (wt.windowTitle.showSkyblockDay) {
            double day = tracker.getDays();
            if (day >= 0) {
                parts.add(String.format(Locale.ROOT, t("babyzombieaddons.windowTitle.display.day"),
                    String.format(Locale.ROOT, "%.1f", day)));
            }
        }

        if (wt.windowTitle.showMemory) {
            Runtime rt = Runtime.getRuntime();
            long used = rt.totalMemory() - rt.freeMemory();
            long max = rt.maxMemory();
            parts.add(formatMB(used) + "/" + formatMB(max));
        }

        if (wt.windowTitle.showSystemMemory) {
            OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long total = os.getTotalMemorySize();
            long used = total - os.getFreeMemorySize();
            parts.add("Sys " + formatMB(used) + "/" + formatMB(total));
        }

        if (wt.windowTitle.showPing) {
            int ping = ServerTick.getPing();
            if (ping >= 0) {
                int windowSec = wt.windowTitle.pingRangeSeconds;
                if (windowSec > 0) {
                    int[] range = ServerTickCounter.getPingRange(windowSec);
                    if (range != null) {
                        parts.add("Ping " + ping + "ms (" + range[0] + "~" + range[1] + ")");
                    } else {
                        parts.add("Ping " + ping + "ms");
                    }
                } else {
                    parts.add("Ping " + ping + "ms");
                }
            }
        }

        if (wt.windowTitle.showSession) {
            parts.add(String.format(Locale.ROOT, t("babyzombieaddons.windowTitle.display.session"),
                formatDuration(ManagementFactory.getRuntimeMXBean().getUptime())));
        }

        if (wt.windowTitle.showIdle) {
            long idleMs = idleMs();
            if (idleMs >= wt.windowTitle.idleThresholdSeconds * 1000L) {
                parts.add(String.format(Locale.ROOT, t("babyzombieaddons.windowTitle.display.idle"),
                    formatDuration(idleMs)));
            }
        }

        if (parts.isEmpty()) {
            if (sb.isEmpty()) return null;
            return sb.toString();
        }

        String firstSep = sb.toString().equals("Hypixel") ? " - " : " | ";
        if (!sb.isEmpty()) {
            sb.append(firstSep);
        }
        sb.append(String.join(" | ", parts));
        return sb.toString();
    }

    // ── Location ──

    private static String buildLocationString(HypixelLocationTracker tracker) {
        if (!tracker.isOnHypixel()) return null;

        if (tracker.isInLimbo()) return "Limbo";

        String lobbyName = tracker.getLobbyName();
        if (lobbyName != null) {
            String serverType = tracker.getServerType();
            if (serverType != null) {
                return serverType.contains("Lobby") ? serverType : serverType + " Lobby";
            }
            return null;
        }

        String serverType = tracker.getServerType();
        String map = tracker.getMap();

        if (serverType != null && map != null) {
            return serverType + " - " + map;
        }
        if (serverType != null) return serverType;
        return null;
    }

    // ── Helpers ──

    private static String t(String key) {
        return Component.translatable(key).getString();
    }

    private static long idleMs() {
        var tracker = Minecraft.getInstance().getFramerateLimitTracker();
        if (tracker == null) return -1;
        return Util.getMillis() - ((FramerateLimitTrackerAccessor) tracker).getLatestInputTime();
    }

    private static String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        long s = ms / 1000;
        if (s < 60) return s + "s";
        long m = s / 60;
        s %= 60;
        if (m < 60) return s == 0 ? m + "m" : m + "m" + s + "s";
        long h = m / 60;
        m %= 60;
        if (m == 0 && s == 0) return h + "h";
        if (s == 0) return h + "h" + m + "m";
        return h + "h" + m + "m" + s + "s";
    }

    private static String formatMB(long bytes) {
        long mb = bytes / (1024 * 1024);
        return String.format("%,dMB", mb);
    }
}
