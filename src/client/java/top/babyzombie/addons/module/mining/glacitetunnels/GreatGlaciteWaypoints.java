package top.babyzombie.addons.module.mining.glacitetunnels;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.render.WorldRenderUtils;
import top.babyzombie.addons.util.render.WorldTextRenderer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class GreatGlaciteWaypoints {
    private static final String CONFIG_FILE = "great_glacite_waypoints.json";
    /** 距主点位多近时显示次级盒子 */
    private static final double SECONDARY_DISTANCE = 5.0;

    // ── Gson model ──
    @SuppressWarnings("unused")
    private static class GlaciteConfig {
        List<GlaciteWaypoint> waypoints;
    }
    @SuppressWarnings("unused")
    private static class GlaciteWaypoint {
        double[] pos, secondary;
        String name = "Great Glacite";
        int[] rgb;
        double size = 1.0, secondarySize = 0.5;
    }

    private static List<GlaciteWaypoint> waypoints = List.of();

    private GreatGlaciteWaypoints() {}

    public static void init() {
        loadConfig();

        RenderPhaseRegister.register(ctx -> {
            if (!ModConfigManager.get().mining.glaciteTunnels.greatGlaciteWaypoints) return;
            if (!isInGlaciteArea()) return;

            var player = Minecraft.getInstance().player;
            for (var w : waypoints) {
                if (w.pos == null || w.pos.length < 3) continue;
                float[] c = rgbToFloats(w.rgb);
                int argb = argbFromRgb(w.rgb);
                double x = w.pos[0], y = w.pos[1], z = w.pos[2];
                WorldRenderUtils.drawFilledBox(ctx,
                    x, y, z,
                    x + w.size, y + w.size, z + w.size,
                    c[0], c[1], c[2], 0.3f, true);

                // 远大近小：与 Waypoints 相同的动态文字缩放
                if (player != null) {
                    double dist = player.position().distanceTo(new Vec3(
                            x + w.size / 2, y + w.size / 2, z + w.size / 2));
                    // float scale = 0.025f + Math.min((float) dist / 100f, 1f) * 0.095f;
                    float scale = Math.clamp((float)dist, 0f, 50f) / 250f;
                    WorldTextRenderer.renderString(ctx, w.name, x + 0.5, y, z + 0.5, argb, scale, true);
                }

                if (w.secondary != null && w.secondary.length >= 3
                        && player != null && player.position().distanceTo(new Vec3(x, y, z)) < SECONDARY_DISTANCE) {
                    double x2 = w.secondary[0], y2 = w.secondary[1], z2 = w.secondary[2];
                    WorldRenderUtils.drawFilledBox(ctx,
                        x2, y2, z2,
                        x2 + w.secondarySize, y2 + w.secondarySize, z2 + w.secondarySize,
                        c[0], c[1], c[2], 1, true);
                    WorldRenderUtils.drawFilledBox(ctx,
                            x2, y2, z2,
                            x2 + w.secondarySize, y2 + w.secondarySize, z2 + w.secondarySize,
                            c[0], c[1], c[2], 0.25f, false);
                }
            }
        });
    }

    /** /bza reload 用：重新从磁盘加载 JSON，返回加载条目数。 */
    public static int reload() {
        loadConfig();
        return waypoints.size();
    }

    private static float[] rgbToFloats(int[] rgb) {
        int[] c = (rgb != null && rgb.length >= 3) ? rgb : new int[]{0, 255, 255};
        return new float[]{c[0] / 255f, c[1] / 255f, c[2] / 255f};
    }

    private static int argbFromRgb(int[] rgb) {
        int[] c = (rgb != null && rgb.length >= 3) ? rgb : new int[]{0, 255, 255};
        return 0xFF000000 | (c[0] << 16) | (c[1] << 8) | c[2];
    }

    private static void loadConfig() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("babyzombieaddons");
        Path configFile = configDir.resolve(CONFIG_FILE);

        if (!Files.exists(configFile)) {
            try {
                Files.createDirectories(configDir);
                var defaultStream = GreatGlaciteWaypoints.class.getResourceAsStream(
                        "/default-config/babyzombieaddons/" + CONFIG_FILE);
                if (defaultStream != null) {
                    Files.copy(defaultStream, configFile);
                }
            } catch (IOException ignored) {}
        }

        try {
            String json = Files.readString(configFile);
            GlaciteConfig root = new Gson().fromJson(json, GlaciteConfig.class);
            if (root != null && root.waypoints != null && !root.waypoints.isEmpty()) {
                waypoints = root.waypoints;
                return;
            }
        } catch (Exception ignored) {
            // fall through to bundled default
        }

        // 兜底：配置文件缺失/损坏/为空时直接用内置默认，避免路径点完全不可用
        try (var defaultStream = GreatGlaciteWaypoints.class.getResourceAsStream(
                "/default-config/babyzombieaddons/" + CONFIG_FILE)) {
            if (defaultStream != null) {
                String json = new String(defaultStream.readAllBytes(), StandardCharsets.UTF_8);
                GlaciteConfig root = new Gson().fromJson(json, GlaciteConfig.class);
                if (root != null && root.waypoints != null) {
                    waypoints = root.waypoints;
                    return;
                }
            }
        } catch (Exception ignored) {}
        waypoints = List.of();
    }

    private static boolean isInGlaciteArea() {
        var t = HypixelLocationTracker.getInstance();
        if (!t.isIn("Dwarven Mines")) return false;
        String loc = t.getLocation();
        return "Dwarven Base Camp".equals(loc) || "Glacite Tunnels".equals(loc) || "Great Glacite Lake".equals(loc);
    }
}
