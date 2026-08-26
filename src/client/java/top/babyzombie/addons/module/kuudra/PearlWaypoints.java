package top.babyzombie.addons.module.kuudra;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.phys.Vec3;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.render.WorldRenderUtils;
import top.babyzombie.addons.util.render.WorldTextRenderer;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 珍珠投掷路径点 — 根据玩家所在区域显示最优投掷位置和时机。
 * 读秒基于补给进度百分比估算。
 */
public final class PearlWaypoints {
    private PearlWaypoints() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("BabyzombieAddons");

    // ── Supply tick table (17 discrete progress percentages, each = 300ms) ──
    private static final int[] SUPPLY_TICKS = {5, 11, 17, 23, 29, 35, 41, 47, 53, 59, 65, 71, 77, 83, 89, 95, 100};
    private static final int TICK_MS = 300;

    // 点位/倒计时仅在玩家水平距离内显示（站位框始终显示）
    private static final double PEARL_SHOW_DISTANCE = 50.0;
    /** 已播过就绪提示音的 waypoint（area+text），就绪状态重置后自动移除 */
    private static final Set<String> alertedWaypoints = new HashSet<>();

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
        double[] pos1, pos2;
        Boolean invertForwardBackward, invertLeftRight;
        List<Waypoint> waypoints;
    }
    @SuppressWarnings("unused")
    private static class Waypoint {
        double[] coords, block;
        int[] rgb;
        double size = 0.4;
        String text = "";
        boolean alert;
        Integer pre, hideForPre;
    }

    private static List<Area> areas = List.of();
    private static Area currentArea;
    private static final List<PearlBox> boxes = new ArrayList<>();
    private static final List<BlockOutline> outlines = new ArrayList<>();
    private static final List<WaypointText> texts = new ArrayList<>();

    private record PearlBox(double x, double y, double z, double half) {}

    public static void openConfigFile() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("babyzombieaddons");
        Path configFile = configDir.resolve("pearl_waypoints.json");
        try {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.EDIT)) {
                java.awt.Desktop.getDesktop().edit(configFile.toFile());
            } else {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", configFile.toString()});
            }
        } catch (IOException ignored) {}
    }

    public static void openIqModrinth() {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://modrinth.com/mod/iq-addons"));
            } else {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", "https://modrinth.com/mod/iq-addons"});
            }
        } catch (IOException ignored) {}
    }

    private record BlockOutline(double x, double y, double z, float r, float g, float b, float a) {}
    private record WaypointText(String text, double x, double y, double z, int color) {}

    public static void init() {
        loadConfig();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var cfg = ModConfigManager.get().kuudra.phase1.pearlWaypoints;
            if (!cfg.enabled) return;
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            if (client.player == null || client.player.tickCount % 2 != 0) return;

            // 站位从进 Kuudra（阶段行未出现）一直显示到阶段二结束；
            // 珍珠点位和倒计时只在阶段一显示
            String phase = getScoreboardPhase(client);
            boolean showStands = "Rescue supplies".equals(phase) || "Protect Elle".equals(phase) || phase.isEmpty();
            boolean showPearls = "Rescue supplies".equals(phase);
            if (!showStands) {
                currentArea = null;
                boxes.clear();
                outlines.clear();
                texts.clear();
                return;
            }
            if (areas.isEmpty()) {
                LOGGER.warn("[BZA] Pearl waypoints enabled but no areas loaded, check pearl_waypoints.json");
                return;
            }
            updateArea(client.player.getX(), client.player.getZ(), showPearls);
        });

        RenderPhaseRegister.register(ctx -> {
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            if (currentArea == null) return;
            var cfg = ModConfigManager.get().kuudra.phase1.pearlWaypoints;

            if (cfg.showBox) {
                for (var b : boxes) {
                    WorldRenderUtils.drawFilledBox(ctx,
                            b.x - b.half, b.y - b.half, b.z - b.half,
                            b.x + b.half, b.y + b.half, b.z + b.half,
                            0, 1, 1, 0.4f, false);
                }
            }
            if (cfg.showOutline) {
                for (var o : outlines) {
                    // 站位方块完整线框，正常深度测试
                    WorldRenderUtils.drawWireframeBox(ctx,
                            o.x, o.y, o.z,
                            o.x + 1, o.y + 1, o.z + 1,
                            o.r, o.g, o.b, o.a, true, 2f);
                }
            }
            if (cfg.showTimer) {
                for (var t : texts) {
                    // 固定 0.15 缩放，不做距离缩放
                    WorldTextRenderer.renderString(ctx, t.text, t.x, t.y, t.z, t.color, 0.15f, true);
                }
            }
        });
    }

    private static String getScoreboardPhase(Minecraft client) {
        if (client.player == null) return "";
        var obj = client.player.level().getScoreboard().getDisplayObjective(DisplaySlot.BY_ID.apply(1));
        if (obj == null) return "";
        for (var holder : client.player.level().getScoreboard().getTrackedPlayers()) {
            if (!client.player.level().getScoreboard().listPlayerScores(holder).containsKey(obj)) continue;
            var team = client.player.level().getScoreboard().getPlayersTeam(holder.getScoreboardName());
            if (team == null) continue;
            String text = ChatUtils.stripColor(ChatUtils.removeEmoji(
                    team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString()))
                    .replaceAll(" \\(.+\\)", "");
            if (text.equals("Rescue supplies") || text.equals("Protect Elle")) return text;
        }
        return "";
    }

    private static void updateArea(double px, double pz, boolean showPearls) {
        boxes.clear();
        outlines.clear();
        texts.clear();
        currentArea = null;

        Area fallback = null;
        double bestDist = Double.MAX_VALUE;
        for (var area : areas) {
            double x1 = Math.min(area.pos1[0], area.pos2[0]);
            double x2 = Math.max(area.pos1[0], area.pos2[0]);
            double z1 = Math.min(area.pos1[1], area.pos2[1]);
            double z2 = Math.max(area.pos1[1], area.pos2[1]);

            if (px >= x1 && px <= x2 && pz >= z1 && pz <= z2) {
                currentArea = area;
                buildWaypoints(area, px, pz, showPearls);
                return;
            }
            // 记录最近区域：玩家不在任何投掷区域内时（如主平台）也显示点位
            double cx = (x1 + x2) / 2, cz = (z1 + z2) / 2;
            double d = (px - cx) * (px - cx) + (pz - cz) * (pz - cz);
            if (d < bestDist) {
                bestDist = d;
                fallback = area;
            }
        }
        if (fallback != null) {
            currentArea = fallback;
            // 不在任何站位区域内（如主平台中间）时只显示站位框，不显示珍珠点位
            buildWaypoints(fallback, px, pz, false);
        }
    }

    private static void buildWaypoints(Area area, double px, double pz, boolean showPearls) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        // IQ 算法：区域内第一个带 block 的 waypoint 作为公共站位中心，所有 waypoint 共用
        Vec3 standCenter = null;
        for (var w : area.waypoints) {
            if (w.block != null && w.block.length == 3) {
                standCenter = new Vec3(w.block[0] + 0.5, w.block[1] + 0.5, w.block[2] + 0.5);
                break;
            }
        }

        for (var w : area.waypoints) {
            if (w.coords == null || w.coords.length < 3) continue;
            double wx = w.coords[0], wy = w.coords[1], wz = w.coords[2];

            // Dynamic Y adjustment: target - (dy*0.81 + dz*0.31*dirF + (-dx*0.63*dirL))
            if (standCenter != null) {
                double dx = px - standCenter.x;
                double dy = player.getY() - standCenter.y;
                double dz = pz - standCenter.z;

                double heightAdjustment = 0;
                if (area.invertForwardBackward != null) {
                    double dirF = area.invertForwardBackward ? -1.0 : 1.0;
                    heightAdjustment = dz * Z_ADJUST * dirF;
                }
                double lateralAdjustment = 0;
                if (area.invertLeftRight != null) {
                    double dirL = area.invertLeftRight ? -1.0 : 1.0;
                    lateralAdjustment = -dx * X_ADJUST * dirL;
                }
                wy -= dy * Y_MULT + heightAdjustment + lateralAdjustment;
            }

            // 点位（盒子）+ 倒计时仅在阶段一且靠近时显示；站位框常显
            boolean near = showPearls && Math.hypot(wx - px, wz - pz) <= PEARL_SHOW_DISTANCE;
            if (near) boxes.add(new PearlBox(wx, wy, wz, w.size * 0.5));

            // Block outline at stand position — 始终显示
            if (w.block != null && w.block.length == 3) {
                float[] c = w.rgb != null ? rgbToFloats(w.rgb) : new float[]{0, 1, 1, 0.6f};
                outlines.add(new BlockOutline(w.block[0], w.block[1], w.block[2], c[0], c[1], c[2], c[3]));
            }

            // Text: supply tick percentage target — 靠近才显示；
            // 进度为 0（没在拿箱子）时隐藏倒计时，只显示站位框
            if (near && !w.text.isEmpty()) {
                double pct = KuudraSupplyProgressHUD.getCurrentProgress();
                if (pct > 0) {
                    int targetPct = Integer.parseInt(w.text);
                    int targetIdx = getTickIndex(targetPct);
                    int curIdx = getTickIndex(pct);
                    int remainingTicks = targetIdx - curIdx;

                    String label;
                    int color;
                    if (remainingTicks <= 0) {
                        label = "§aREADY";
                        color = 0xFF00FF00;
                        // 就绪提示音（独立开关）
                        String alertKey = area.name + "_" + w.text;
                        if (alertedWaypoints.add(alertKey) && w.alert
                                && ModConfigManager.get().kuudra.phase1.pearlWaypoints.throwAlert) {
                            var p = Minecraft.getInstance().player;
                            if (p != null) {
                                p.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), 1.3f, 1.6f);
                            }
                        }
                    } else {
                        alertedWaypoints.remove(area.name + "_" + w.text);
                        double remainingSec = (remainingTicks * TICK_MS) / 1000.0;
                        label = String.format("§e%.1fs", remainingSec);
                        color = 0xFFFFFF00;
                    }
                    texts.add(new WaypointText(label, wx, wy + 0.5, wz, color));
                }
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
            if (root != null && root.areas != null && !root.areas.isEmpty()) {
                areas = root.areas;
                return;
            }
        } catch (Exception ignored) {
            // fall through to bundled default
        }

        // 兜底：配置文件缺失/损坏/为空时直接用内置默认，避免路径点完全不可用
        try (var defaultStream = PearlWaypoints.class.getResourceAsStream(
                "/default-config/babyzombieaddons/pearl_waypoints.json")) {
            if (defaultStream != null) {
                String json = new String(defaultStream.readAllBytes(), StandardCharsets.UTF_8);
                PearlConfig root = new Gson().fromJson(json, PearlConfig.class);
                if (root != null && root.areas != null) {
                    areas = root.areas;
                    return;
                }
            }
        } catch (Exception ignored) {}
        areas = List.of();
    }
}
