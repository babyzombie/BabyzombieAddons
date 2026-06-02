package top.babyzombie.addons.config.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
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

    private HudManager() {}

    public static void init() {
        load();
    }

    public static void register(String name, int defaultX, int defaultY, float defaultScale,
                                 int width, int height, String demoText,
                                 java.util.function.BooleanSupplier showCondition) {
        HudElement e = new HudElement();
        e.name = name; e.x = defaultX; e.y = defaultY; e.scale = defaultScale;
        e.width = width; e.height = height; e.demoText = demoText; e.showCondition = showCondition;
        elements.put(name, e);
    }

    public static int x(String name) { var e = elements.get(name); return e != null ? e.x : 0; }
    public static int y(String name) { var e = elements.get(name); return e != null ? e.y : 0; }
    public static float scale(String name) { var e = elements.get(name); return e != null ? e.scale : 1.0f; }

    public static boolean shouldShow(String name) {
        var e = elements.get(name);
        return e != null && e.showCondition.getAsBoolean();
    }

    public static void drawScaled(net.minecraft.client.gui.GuiGraphics gui, net.minecraft.client.gui.Font font, String text, int x, int y, String name) {
        drawScaled(gui, font, text, x, y, scale(name));
    }

    public static void drawScaled(net.minecraft.client.gui.GuiGraphics gui, net.minecraft.client.gui.Font font, String text, int x, int y, float s) {
        if (s != 1f) {
            var ps = gui.pose();
            ps.pushMatrix();
            ps.translate((float) x, (float) y);
            ps.scale(s, s);
            gui.drawString(font, text, 0, 0, 0xFFFFFFFF, true);
            ps.popMatrix();
        } else {
            gui.drawString(font, text, x, y, 0xFFFFFFFF, true);
        }
    }

    static String getDemoText(String name) {
        var e = elements.get(name);
        return e != null ? e.demoText : "";
    }

    public static void openEditScreen(Screen parent) {
        Minecraft.getInstance().setScreen(new HudEditScreen(parent));
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
                        e.x = (int) entry.getValue()[0];
                        e.y = (int) entry.getValue()[1];
                        e.scale = entry.getValue()[2];
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
        String name, demoText;
        int x, y, width, height;
        float scale;
        java.util.function.BooleanSupplier showCondition;
    }
}
