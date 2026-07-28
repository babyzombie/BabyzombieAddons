package top.babyzombie.addons.util;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ModConfigManager;

import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which mod rendered each region of the HUD during {@code Gui.extractRenderState()}.
 * Used by {@link top.babyzombie.addons.config.MiscConfig#showExternalHudSource}.
 */
public final class HudSourceTracker {

    private HudSourceTracker() {}

    // ── Frame state (ThreadLocal for render-thread safety) ──
    private static final ThreadLocal<Boolean> TRACKING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<List<RegionEntry>> REGIONS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<GuiGraphicsExtractor> CURRENT_GUI = new ThreadLocal<>();
    private static final ThreadLocal<String> LAST_CALLER = new ThreadLocal<>();

    // ── Global caches ──
    private static final Map<String, String> MOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> FILTER_CACHE = new ConcurrentHashMap<>();

    // ── Constants ──
    private static final String SELF_PKG = "top.babyzombie.addons";
    private static final int MERGE_GAP = 8;

    private static final java.lang.StackWalker STACK_WALKER =
            java.lang.StackWalker.getInstance(java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE);

    // ================================================================
    //  Public API — called from Mixins
    // ================================================================

    public static void startFrame(GuiGraphicsExtractor gui) {
        if (!isEnabled()) return;
        TRACKING.set(true);
        REGIONS.get().clear();
        CURRENT_GUI.set(gui);
        LAST_CALLER.remove();
    }

    public static void endFrame() {
        // Tracking stops; regions persist for HudEditScreen.renderTooltipFromScreen()
        TRACKING.set(false);
        CURRENT_GUI.remove();
        LAST_CALLER.remove();
    }

    /**
     * Renders the hover tooltip if the mouse is over an external HUD region.
     * Called from HudEditScreen.extractRenderState so it appears above the edit overlay.
     */
    /**
     * Renders the hover tooltip if the mouse is over an external HUD region.
     * Called from HudEditScreen.extractRenderState so it appears above the edit overlay.
     *
     * @param skipIfOwnElement if true, skips rendering when the mouse is over one of
     *                         our own HUD elements (to avoid overlapping tooltips)
     * @return true if an external region was hit
     */
    public static boolean renderTooltipFromScreen(GuiGraphicsExtractor gui, Font font, int mx, int my, boolean skipIfOwnElement) {
        if (!isEnabled()) return false;
        if (skipIfOwnElement) return false;
        List<RegionEntry> list = REGIONS.get();
        if (list.isEmpty()) return false;

        RegionEntry hit = null;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).contains(mx, my)) {
                hit = list.get(i);
                break;
            }
        }
        if (hit != null) {
            drawHighlight(gui, hit);
            drawTooltip(gui, font, hit, mx, my);
            return true;
        }
        return false;
    }

    public static boolean isTracking() {
        return TRACKING.get();
    }

    /**
     * Record a draw-call bounding box. Coordinates are in LOCAL space (before PoseStack).
     * The current pose transform is applied to get screen-space coordinates.
     */
    public static void record(int x1, int y1, int x2, int y2) {
        if (!TRACKING.get()) return;

        // Apply current PoseStack transform to get screen coordinates
        GuiGraphicsExtractor gui = CURRENT_GUI.get();
        if (gui != null) {
            var m = gui.pose();
            float sx1 = m.m00() * x1 + m.m10() * y1 + m.m20();
            float sy1 = m.m01() * x1 + m.m11() * y1 + m.m21();
            float sx2 = m.m00() * x2 + m.m10() * y2 + m.m20();
            float sy2 = m.m01() * x2 + m.m11() * y2 + m.m21();
            x1 = (int) Math.min(sx1, sx2);
            y1 = (int) Math.min(sy1, sy2);
            x2 = (int) Math.max(sx1, sx2);
            y2 = (int) Math.max(sy1, sy2);
        }

        List<RegionEntry> list = REGIONS.get();

        // Fast path: spatially close to last entry from same caller → expand it
        String lastCaller = LAST_CALLER.get();
        if (lastCaller != null && !list.isEmpty()) {
            RegionEntry last = list.getLast();
            if (lastCaller.equals(last.callerClass) && isNear(last, x1, y1, x2, y2)) {
                last.expand(x1, y1, x2, y2);
                return;
            }
        }

        String callerClass = resolveCaller(x1, y1, x2, y2);
        if (callerClass == null) return;

        String modName = MOD_CACHE.get(callerClass);
        if (modName == null) return;

        LAST_CALLER.set(callerClass);

        if (!list.isEmpty()) {
            RegionEntry last = list.getLast();
            if (callerClass.equals(last.callerClass) && isNear(last, x1, y1, x2, y2)) {
                last.expand(x1, y1, x2, y2);
                return;
            }
        }

        list.add(new RegionEntry(x1, y1, x2, y2, callerClass, modName));
    }

    public static boolean isEnabled() {
        if (!ModConfigManager.get().misc.showExternalHudSource) return false;
        // Only active when the HUD edit screen is open
        var screen = Minecraft.getInstance().screen;
        return screen instanceof top.babyzombie.addons.config.hud.HudEditScreen;
    }

    // ================================================================
    //  Caller identification
    // ================================================================

    private static String resolveCaller(int x1, int y1, int x2, int y2) {
        String last = LAST_CALLER.get();
        if (last != null && !REGIONS.get().isEmpty()) {
            RegionEntry lastEntry = REGIONS.get().getLast();
            if (isNear(lastEntry, x1, y1, x2, y2)) {
                return last;
            }
        }
        return STACK_WALKER.walk(frames ->
                frames.map(java.lang.StackWalker.StackFrame::getClassName)
                        .filter(HudSourceTracker::passesFilter)
                        .filter(cls -> MOD_CACHE.computeIfAbsent(cls, HudSourceTracker::resolveModByName) != null)
                        .findFirst()
                        .orElse(null)
        );
    }

    private static boolean passesFilter(String className) {
        return FILTER_CACHE.computeIfAbsent(className, cls -> {
            if (cls.startsWith("net.minecraft")) return false;
            if (cls.startsWith("java.")) return false;
            if (cls.startsWith("jdk.")) return false;
            if (cls.startsWith("sun.")) return false;
            if (cls.startsWith("com.mojang.")) return false;
            if (cls.startsWith("org.spongepowered.asm")) return false;
            if (cls.startsWith("net.fabricmc")) return false;
            if (cls.startsWith("org.lwjgl")) return false;
            if (cls.startsWith("org.jetbrains")) return false;
            if (cls.startsWith("kotlin.")) return false;
            if (cls.startsWith("com.modrinth")) return false;
            if (cls.contains("$$")) return false;
            if (cls.startsWith(SELF_PKG)) return false;
            return true;
        });
    }

    /** Resolves a class name to its mod name by matching CodeSource against mod origins. */
    private static String resolveModByName(String className) {
        try {
            Class<?> cls = Class.forName(className, false, HudSourceTracker.class.getClassLoader());
            Path classPath = codeSourcePath(cls);
            if (classPath == null) return null;

            String classPathReal = classPath.toRealPath().toString();
            for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                ModOrigin origin = mod.getOrigin();
                if (origin.getKind() != ModOrigin.Kind.PATH) continue;
                for (Path op : origin.getPaths()) {
                    try {
                        if (classPathReal.equals(op.toRealPath().toString())) {
                            return mod.getMetadata().getName();
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Path codeSourcePath(Class<?> cls) {
        try {
            ProtectionDomain pd = cls.getProtectionDomain();
            if (pd == null) return null;
            CodeSource cs = pd.getCodeSource();
            if (cs == null) return null;
            URL loc = cs.getLocation();
            if (loc == null) return null;

            String urlStr = loc.toString();
            if (urlStr.startsWith("jar:")) {
                urlStr = urlStr.substring(4, urlStr.indexOf("!/"));
            }
            return Path.of(java.net.URI.create(urlStr));
        } catch (Exception e) {
            return null;
        }
    }

    // ================================================================
    //  Spatial / rendering
    // ================================================================

    private static boolean isNear(RegionEntry last, int x1, int y1, int x2, int y2) {
        return !(last.x2 + MERGE_GAP < x1 || x2 + MERGE_GAP < last.x1
                || last.y2 + MERGE_GAP < y1 || y2 + MERGE_GAP < last.y1);
    }

    private static void drawHighlight(GuiGraphicsExtractor gui, RegionEntry entry) {
        int c = 0xCC00FF00; // bright green, semi-transparent
        // top
        gui.fill(entry.x1, entry.y1, entry.x2, entry.y1 + 1, c);
        // bottom
        gui.fill(entry.x1, entry.y2 - 1, entry.x2, entry.y2, c);
        // left
        gui.fill(entry.x1, entry.y1, entry.x1 + 1, entry.y2, c);
        // right
        gui.fill(entry.x2 - 1, entry.y1, entry.x2, entry.y2, c);
    }

    private static void drawTooltip(GuiGraphicsExtractor gui, Font font, RegionEntry entry, int mx, int my) {
        String text = Component.translatable("babyzombieaddons.hud.externalSource",
                entry.modName, shortName(entry.callerClass)).getString();
        int textW = font.width(text);
        int pad = 4;
        int tooltipW = textW + pad * 2;
        int tooltipH = font.lineHeight + pad * 2;

        var window = Minecraft.getInstance().getWindow();
        int sw = window.getGuiScaledWidth();
        int sh = window.getGuiScaledHeight();
        int tx = Math.clamp(mx + 12, 0, sw - tooltipW);
        int ty = Math.clamp(my - tooltipH / 2, 0, sh - tooltipH);

        gui.fill(tx, ty, tx + tooltipW, ty + tooltipH, 0xC0222222);
        gui.fill(tx, ty, tx + tooltipW, ty + 1, 0xCC00FFFF);
        gui.text(font, text, tx + pad, ty + pad, 0xFFFFFFFF, false);
    }

    private static String shortName(String className) {
        int dot = className.lastIndexOf('.');
        return dot >= 0 ? className.substring(dot + 1) : className;
    }

    // ================================================================
    //  Data classes
    // ================================================================

    static class RegionEntry {
        int x1, y1, x2, y2;
        final String callerClass;
        final String modName;

        RegionEntry(int x1, int y1, int x2, int y2, String callerClass, String modName) {
            this.x1 = x1; this.y1 = y1;
            this.x2 = x2; this.y2 = y2;
            this.callerClass = callerClass;
            this.modName = modName;
        }

        boolean contains(int mx, int my) {
            return mx >= x1 && mx <= x2 && my >= y1 && my <= y2;
        }

        void expand(int ox1, int oy1, int ox2, int oy2) {
            x1 = Math.min(x1, ox1);
            y1 = Math.min(y1, oy1);
            x2 = Math.max(x2, ox2);
            y2 = Math.max(y2, oy2);
        }
    }
}
