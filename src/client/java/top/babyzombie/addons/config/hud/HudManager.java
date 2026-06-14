package top.babyzombie.addons.config.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HudManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("babyzombieaddons").resolve("hud.json");

    static final Map<String, HudElement> elements = new LinkedHashMap<>();
    private static Map<String, float[]> loaded;

    private HudManager() {}

    public static void init() {
        loaded = loadRaw();
    }

    public static void register(String name, int defaultX, int defaultY, float defaultScale,
                                 String demoText, String labelKey,
                                 java.util.function.BooleanSupplier showCondition) {
        HudElement e = new HudElement();
        e.name = name; e.x = defaultX; e.y = defaultY; e.scale = defaultScale;
        e.demoText = demoText; e.labelKey = labelKey; e.showCondition = showCondition;
        if (loaded != null) {
            float[] saved = loaded.get(name);
            if (saved != null && saved.length >= 3) {
                e.x = (int) saved[0];
                e.y = (int) saved[1];
                e.scale = saved[2];
            }
        }
        elements.put(name, e);
    }

    public static int x(String name) { var e = elements.get(name); return e != null ? e.x : 0; }
    public static int y(String name) { var e = elements.get(name); return e != null ? e.y : 0; }
    public static float scale(String name) { var e = elements.get(name); return e != null ? e.scale : 1.0f; }

    public static boolean shouldShow(String name) {
        var e = elements.get(name);
        return e != null && e.showCondition.getAsBoolean();
    }

    public static void drawScaled(GuiGraphicsExtractor gui, Font font, String text, int x, int y, String name) {
        drawScaled(gui, font, text, x, y, scale(name));
    }

    public static void drawScaled(GuiGraphicsExtractor gui, Font font, String text, int x, int y, float s) {
        String[] lines = text.split("\n", -1);
        if (s != 1f) {
            var ps = gui.pose();
            ps.pushMatrix();
            ps.translate((float) x, (float) y);
            ps.scale(s, s);
            int lineH = font.lineHeight;
            for (int i = 0; i < lines.length; i++) {
                gui.text(font, lines[i], 0, i * lineH, 0xFFFFFFFF, true);
            }
            ps.popMatrix();
        } else {
            int lineH = font.lineHeight;
            for (int i = 0; i < lines.length; i++) {
                gui.text(font, lines[i], x, y + i * lineH, 0xFFFFFFFF, true);
            }
        }
    }

    static String getDemoText(String name) {
        var e = elements.get(name);
        return e != null ? e.demoText : "";
    }

    static String getLabelKey(String name) {
        var e = elements.get(name);
        return e != null ? e.labelKey : "";
    }

    public static void openEditScreen(Screen parent) {
        Minecraft.getInstance().setScreenAndShow(new HudEditScreen(parent));
    }

    private static Map<String, float[]> loadRaw() {
        if (!Files.exists(SAVE_FILE)) return null;
        try {
            String json = Files.readString(SAVE_FILE);
            return GSON.fromJson(json, new TypeToken<Map<String, float[]>>(){}.getType());
        } catch (IOException ignored) {
            return null;
        }
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
        String name, demoText, labelKey;
        int x, y;
        float scale;
        java.util.function.BooleanSupplier showCondition;
    }
}
