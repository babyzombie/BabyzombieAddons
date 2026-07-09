package top.babyzombie.addons.module.misc;

import com.sun.management.OperatingSystemMXBean;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public final class WindowTitleModule {

    /// 缓存 createTitle() 返回的原始窗口标题
    public static String cachedOriginalTitle;

    private WindowTitleModule() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(WindowTitleModule::onTick);
    }

    private static void onTick(Minecraft client) {
        ModConfig.WindowTitleConfig wt = ModConfigManager.get().windowTitle;
        if (!wt.enabled) return;
        if (client.player == null) return;

        int interval = Math.clamp(wt.updateInterval, 1, 20);
        if (client.player.tickCount % interval != 0) return;

        String title = buildTitle(cachedOriginalTitle);
        if (title != null && client.getWindow() != null) {
            client.getWindow().setTitle(title);
        }
    }

    /// 拼装窗口标题，返回 null 表示不要修改
    public static String buildTitle(String originalTitle) {
        ModConfig.WindowTitleConfig wt = ModConfigManager.get().windowTitle;
        if (!wt.enabled) return null;

        StringBuilder sb = new StringBuilder();

        HypixelLocationTracker tracker = HypixelLocationTracker.getInstance();

        // ── 前缀 ──
        if (wt.overrideOriginal && wt.showLocation) {
            // 覆盖模式仅在位置显示开启时生效：在 Hypixel 用 "Hypixel" 顶替原前缀
            if (tracker.isOnHypixel()) {
                sb.append("Hypixel");
            }
        } else {
            if (originalTitle != null && !originalTitle.isEmpty()) {
                sb.append(originalTitle);
            }
        }

        // ── 自定义片段（位置 → 内存 → 延迟）──
        List<String> parts = new ArrayList<>();

        if (wt.showLocation) {
            String loc = buildLocationString(tracker);
            if (loc != null) {
                parts.add(loc);
            }
        }

        if (wt.showMemory) {
            Runtime rt = Runtime.getRuntime();
            long used = rt.totalMemory() - rt.freeMemory();
            long max = rt.maxMemory();
            parts.add(formatMB(used) + "/" + formatMB(max));
        }

        if (wt.showSystemMemory) {
            OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long total = os.getTotalMemorySize();
            long used = total - os.getFreeMemorySize();
            parts.add("Sys " + formatMB(used) + "/" + formatMB(total));
        }

        if (wt.showPing) {
            int ping = ServerTick.getPing();
            if (ping >= 0) {
                parts.add("Ping " + ping + "ms");
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

    private static String formatMB(long bytes) {
        long mb = bytes / (1024 * 1024);
        return String.format("%,dMB", mb);
    }
}
