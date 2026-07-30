package top.babyzombie.addons.module.kuudra;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.render.WorldRenderUtils;
import top.babyzombie.addons.util.render.WorldTextRenderer;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 珍珠投掷路径点 — 根据玩家所在区域显示最优投掷位置和时机。
 * 读秒基于补给进度百分比估算。
 */
public final class PearlWaypoints {
    private PearlWaypoints() {}

    // ── Supply tick table (17 discrete progress percentages, each = 300ms) ──
    private static final int[] SUPPLY_TICKS = {5, 11, 17, 23, 29, 35, 41, 47, 53, 59, 65, 71, 77, 83, 89, 95, 100};
    private static final int TICK_MS = 300;

    // ── Dynamic adjustment factors ──
    private static final double Y_MULT = 0.81;
    private static final double Z_ADJUST = 0.31;
    private static final double X_ADJUST = 0.63;

    // ── Gson model ──
    @SuppressWarnings("unused")
    private static class PearlConfig {
        List<Area> areas;
    }
    @SuppressWarnings("unused")
    private static class Area {
        String name;
        int[] pos1, pos2;
        Boolean invertForwardBackward, invertLeftRight;
        List<Waypoint> waypoints;
    }
    @SuppressWarnings("unused")
    private static class Waypoint {
        int[] coords, rgb, block;
        double size = 0.4;
        String text = "";
        boolean alert;
        Integer pre, hideForPre;
    }

    private static List<Area> areas = List.of();
    private static Area currentArea;
    private static final List<Vec3> boxes = new ArrayList<>();
    private static final List<BlockOutline> outlines = new ArrayList<>();
    private static final List<WaypointText> texts = new ArrayList<>();

    private record BlockOutline(double x, double y, double z, float r, float g, float b, float a) {}
    private record WaypointText(String text, double x, double y, double z, int color) {}

    public static void init() {
        loadConfig();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().kuudra.phase1.pearlWaypoints) return;
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            if (!KuudraSupplyProgressHUD.isInSuppliesPhase()) return;
            if (areas.isEmpty()) return;

            if (client.player == null || client.player.tickCount % 10 != 0) return;
            updateArea(client.player.getX(), client.player.getZ());
        });

        RenderPhaseRegister.register(ctx -> {
            if (currentArea == null) return;

            double pct = KuudraSupplyProgressHUD.getCurrentProgress();

            for (var b : boxes) {
                // Filled box at waypoint coords
                WorldRenderUtils.drawFilledBox(ctx,
                        b.x - 0.3, b.y - 0.3, b.z - 0.3,
                        b.x + 0.3, b.y + 0.3, b.z + 0.3,
                        0, 1, 1, 0.4f, true);
            }
            for (var o : outlines) {
                WorldRenderUtils.drawWireframeBox(ctx,
                        o.x - 0.3, o.y, o.z - 0.3,
                        o.x + 0.3, o.y + 1.8, o.z + 0.3,
                        o.r, o.g, o.b, o.a, true, 2f);
            }
            for (var t : texts) {
                WorldTextRenderer.renderString(ctx, t.text, t.x, t.y, t.z, t.color, 0.04f, true);
            }
        });
    }

    private static void updateArea(double px, double pz) {
        boxes.clear();
        outlines.clear();
        texts.clear();
        currentArea = null;

        for (var area : areas) {
            double x1 = Math.min(area.pos1[0], area.pos2[0]);
            double x2 = Math.max(area.pos1[0], area.pos2[0]);
            double z1 = Math.min(area.pos1[1], area.pos2[1]);
            double z2 = Math.max(area.pos1[1], area.pos2[1]);

            if (px >= x1 && px <= x2 && pz >= z1 && pz <= z2) {
                currentArea = area;
                buildWaypoints(area, px, pz);
                break;
            }
        }
    }

    private static void buildWaypoints(Area area, double px, double pz) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        for (var w : area.waypoints) {
            double wx = w.coords[0], wy = w.coords[1], wz = w.coords[2];
            boolean hasBlock = w.block != null && w.block.length == 3;

            // Dynamic Y adjustment
            if (hasBlock) {
                double bx = w.block[0], by = w.block[1], bz = w.block[2];
                double dx = px - bx, dy = player.getY() - by, dz = pz - bz;

                double fwd = (area.invertForwardBackward != null && area.invertForwardBackward) ? -dz : dz;
                double lr  = (area.invertLeftRight != null && area.invertLeftRight) ? -dx : dx;

                if (area.invertForwardBackward == null) fwd = 0;
                if (area.invertLeftRight == null) lr = 0;

                wy += fwd * Z_ADJUST + lr * X_ADJUST + dy * Y_MULT;
            }

            double sz = w.size * 0.5;
            boxes.add(new Vec3(wx, wy, wz));

            // Block outline at stand position
            if (hasBlock) {
                float[] c = w.rgb != null ? rgbToFloats(w.rgb) : new float[]{0, 1, 1, 0.6f};
                outlines.add(new BlockOutline(w.block[0], w.block[1], w.block[2], c[0], c[1], c[2], c[3]));
            }

            // Text: supply tick percentage target
            if (!w.text.isEmpty()) {
                double pct = KuudraSupplyProgressHUD.getCurrentProgress();
                int targetPct = Integer.parseInt(w.text);
                int targetIdx = getTickIndex(targetPct);
                int curIdx = getTickIndex(pct);
                int remainingTicks = targetIdx - curIdx;

                String label;
                int color;
                if (remainingTicks <= 0) {
                    label = "§aREADY";
                    color = 0x00FF00;
                } else {
                    double remainingSec = (remainingTicks * TICK_MS) / 1000.0;
                    label = String.format("§e%.1fs", remainingSec);
                    color = 0xFFFF00;
                }
                texts.add(new WaypointText(label, wx, wy + 0.5, wz, color));

                // Also show the target %
                texts.add(new WaypointText("§7" + w.text + "%", wx, wy + 0.1, wz, 0xAAAAAA));
            }
        }
    }

    private static int getTickIndex(double percentage) {
        for (int i = 0; i < SUPPLY_TICKS.length; i++) {
            if (percentage <= SUPPLY_TICKS[i]) return i;
        }
        return SUPPLY_TICKS.length - 1;
    }

    private static float[] rgbToFloats(int[] rgb) {
        return new float[]{
                rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f, 0.7f
        };
    }

    private static void loadConfig() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("babyzombieaddons");
        Path configFile = configDir.resolve("pearl_waypoints.json");

        if (!Files.exists(configFile)) {
            try {
                Files.createDirectories(configDir);
                var defaultStream = PearlWaypoints.class.getResourceAsStream(
                        "/default-config/babyzombieaddons/pearl_waypoints.json");
                if (defaultStream != null) {
                    Files.copy(defaultStream, configFile);
                }
            } catch (IOException ignored) {}
        }

        try {
            String json = Files.readString(configFile);
            PearlConfig root = new Gson().fromJson(json, PearlConfig.class);
            if (root != null && root.areas != null) {
                areas = root.areas;
            }
        } catch (Exception e) {
            areas = List.of();
        }
    }
}
