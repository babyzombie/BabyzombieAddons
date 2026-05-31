package top.babyzombie.addons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HUD position manager — allows dragging and scaling HUD elements.
 * Enter edit mode via settings page button, then drag/scroll elements.
 */
public final class HudManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("babyzombieaddons").resolve("hud.json");

    static final Map<String, HudElement> elements = new LinkedHashMap<>();
    static String selected;
    static boolean editMode;

    private HudManager() {}

    public static void init() {
        load();
    }

    public static void register(String name, int defaultX, int defaultY, float defaultScale,
                                 int width, int height, java.util.function.BooleanSupplier showCondition) {
        HudElement e = new HudElement();
        e.name = name; e.x = defaultX; e.y = defaultY; e.scale = defaultScale;
        e.width = width; e.height = height; e.showCondition = showCondition;
        e.defaultX = defaultX; e.defaultY = defaultY; e.defaultScale = defaultScale;
        elements.put(name, e);
    }

    public static int x(String name) { var e = elements.get(name); return e != null ? e.x : 0; }
    public static int y(String name) { var e = elements.get(name); return e != null ? e.y : 0; }
    public static float scale(String name) { var e = elements.get(name); return e != null ? e.scale : 1.0f; }
    public static boolean shouldShow(String name) {
        var e = elements.get(name);
        return e != null && e.showCondition.getAsBoolean();
    }

    public static boolean isEditMode() { return editMode; }
    public static void toggleEditMode() {
        editMode = !editMode;
        if (!editMode) { selected = null; save(); }
    }

    public static void renderEditOverlay(GuiGraphics gui, int mouseX, int mouseY) {
        if (!editMode) return;
        gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), 0x46000000);
        for (HudElement e : elements.values()) {
            if (!e.showCondition.getAsBoolean()) continue;
            int w = (int)(e.width * e.scale), h = (int)(e.height * e.scale);
            gui.fill(e.x, e.y, e.x + w, e.y + h, e.name.equals(selected) ? 0x3CFFFFFF : 0x2D000000);
        }
    }

    public static boolean handleClick(double mx, double my) {
        if (!editMode) return false;
        for (HudElement e : elements.values()) {
            if (!e.showCondition.getAsBoolean()) continue;
            int w = (int)(e.width * e.scale), h = (int)(e.height * e.scale);
            if (mx >= e.x && mx <= e.x + w && my >= e.y && my <= e.y + h) {
                selected = e.name; return true;
            }
        }
        selected = null; return true;
    }

    public static void handleDrag(double mx, double my) {
        if (!editMode || selected == null) return;
        HudElement e = elements.get(selected);
        if (e == null) return;
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int w = (int)(e.width * e.scale), h = (int)(e.height * e.scale);
        e.x = (int)Math.max(0, Math.min(mx - w / 2.0, sw - w));
        e.y = (int)Math.max(0, Math.min(my - h / 2.0, sh - h));
    }

    public static void handleScroll(double scrollY) {
        if (!editMode || selected == null) return;
        HudElement e = elements.get(selected);
        if (e != null) e.scale = Math.max(0.3f, Math.min(e.scale + (float)(scrollY / 10.0), 5.0f));
    }

    private static void load() {
        if (!Files.exists(SAVE_FILE)) return;
        try {
            String json = Files.readString(SAVE_FILE);
            Map<String, float[]> saved = GSON.fromJson(json, new TypeToken<Map<String, float[]>>(){}.getType());
            if (saved != null) {
                for (var entry : saved.entrySet()) {
                    HudElement e = elements.get(entry.getKey());
                    if (e != null && entry.getValue().length >= 3) {
                        e.x = (int)entry.getValue()[0]; e.y = (int)entry.getValue()[1]; e.scale = entry.getValue()[2];
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    static void save() {
        Map<String, float[]> data = new LinkedHashMap<>();
        for (var entry : elements.entrySet()) {
            HudElement e = entry.getValue();
            data.put(entry.getKey(), new float[]{e.x, e.y, e.scale});
        }
        try {
            Files.createDirectories(SAVE_FILE.getParent());
            Files.writeString(SAVE_FILE, GSON.toJson(data));
        } catch (IOException ignored) {}
    }

    static class HudElement {
        String name;
        int x, y, width, height, defaultX, defaultY;
        float scale, defaultScale;
        java.util.function.BooleanSupplier showCondition;
    }
}
