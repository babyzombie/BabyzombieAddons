package top.babyzombie.addons.module.misc;

import com.sun.management.OperatingSystemMXBean;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import top.babyzombie.addons.config.GeneralConfig.WindowTitle.WindowTitleElement;
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

    /// 拼装窗口标题，返回 null 表示功能关闭不修改；启用但无内容时返回空串
    public static String buildTitle(String originalTitle) {
        var wt = ModConfigManager.get().general;
        if (!wt.windowTitle.enabled) return null;

        HypixelLocationTracker tracker = HypixelLocationTracker.getInstance();
        var elements = wt.windowTitle.elements;
        if (elements == null) return "";

        List<String> parts = new ArrayList<>();
        int hypixelIndex = -1;
        boolean hypixelFollowedByLocation = false;

        // 按用户在 DraggableList 中的顺序逐个添加
        for (WindowTitleElement element : elements) {
            switch (element) {
                case ORIGINAL_TITLE -> {
                    if (wt.windowTitle.overrideOriginal
                        && elements.contains(WindowTitleElement.LOCATION)
                        && tracker.isOnHypixel()) {
                        parts.add("Hypixel");
                        hypixelIndex = parts.size() - 1;
                    } else if (originalTitle != null && !originalTitle.isEmpty()) {
                        parts.add(originalTitle);
                    }
                }
                case LOCATION -> {
                    String loc = buildLocationString(tracker);
                    if (loc != null) {
                        parts.add(loc);
                        if (hypixelIndex >= 0 && hypixelIndex + 1 == parts.size() - 1) {
                            hypixelFollowedByLocation = true;
                        }
                    }
                }
                case DAY -> {
                    double day = tracker.getDays();
                    if (day >= 0) {
                        parts.add(String.format(Locale.ROOT, t("babyzombieaddons.windowTitle.display.day"),
                            String.format(Locale.ROOT, "%.1f", day)));
                    }
                }
                case JVM_MEMORY -> {
                    Runtime rt = Runtime.getRuntime();
                    long used = rt.totalMemory() - rt.freeMemory();
                    long max = rt.maxMemory();
                    parts.add(formatMB(used) + "/" + formatMB(max));
                }
                case SYSTEM_MEMORY -> {
                    OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                    long total = os.getTotalMemorySize();
                    long used = total - os.getFreeMemorySize();
                    parts.add("Sys " + formatMB(used) + "/" + formatMB(total));
                }
                case CPU -> {
                    OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                    double cpu = os.getCpuLoad();
                    if (cpu >= 0) {
                        parts.add(String.format(Locale.ROOT, t("babyzombieaddons.windowTitle.display.cpu"),
                            String.format(Locale.ROOT, "%.0f%%", cpu * 100)));
                    }
                }
                case PING -> {
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
                case SESSION -> parts.add(String.format(Locale.ROOT, t("babyzombieaddons.windowTitle.display.session"),
                    formatDuration(ManagementFactory.getRuntimeMXBean().getUptime())));
                case IDLE -> {
                    long idleMs = idleMs();
                    if (idleMs >= wt.windowTitle.idleThresholdSeconds * 1000L) {
                        parts.add(String.format(Locale.ROOT, t("babyzombieaddons.windowTitle.display.idle"),
                            formatDuration(idleMs)));
                    }
                }
            }
        }

        if (parts.isEmpty()) return "";

        String separator = wt.windowTitle.separator;
        if (separator == null) separator = " | ";

        // 只有 “Hypixel” 后面紧跟位置信息时才用 “ - ”，其余统一用玩家配置的连接符
        StringBuilder sb = new StringBuilder(parts.get(0));
        for (int i = 1; i < parts.size(); i++) {
            if (hypixelFollowedByLocation && i == hypixelIndex + 1) {
                sb.append(" - ");
            } else {
                sb.append(separator);
            }
            sb.append(parts.get(i));
        }
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
