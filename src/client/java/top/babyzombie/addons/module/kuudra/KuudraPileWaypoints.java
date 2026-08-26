package top.babyzombie.addons.module.kuudra;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.DisplaySlot;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.render.BeamRenderer;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.render.WorldRenderUtils;
import top.babyzombie.addons.util.render.WorldTextRenderer;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 补给放置点（Pile）路径点 — 每个放置点显示对应的 pre spot 名称（X / Triangle / Equals / Slash…），
 * 并标记缺失 pre 的放置点。数据参考 IQ Addons 的 pile_locations.json。
 */
public final class KuudraPileWaypoints {
    private KuudraPileWaypoints() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("BabyzombieAddons");

    private static final int PILE_BEACON_HEIGHT = 40;

    private record Pile(String name, double x, double y, double z) {}

    private static List<Pile> piles = List.of();
    private static boolean inSuppliesPhase;
    /** 已放置补给（出现 "SUPPLIES RECEIVED" 盔甲架）的 pile 索引，不再显示 */
    private static final Set<Integer> completedPiles = new HashSet<>();

    public static void init() {
        loadConfig();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var cfg = ModConfigManager.get().kuudra.phase1;
            if (!cfg.supplyPileWaypoints) return;
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            if (client.player == null || client.player.tickCount % 20 != 0) return;
            inSuppliesPhase = "Rescue supplies".equals(getScoreboardPhase(client));

            // 检测已放置补给的 pile（放置完成后出现 "SUPPLIES RECEIVED" 盔甲架）
            completedPiles.clear();
            for (var s : client.player.level().getEntitiesOfClass(ArmorStand.class,
                    new AABB(client.player.blockPosition()).inflate(64),
                    e -> e.hasCustomName() && ChatUtils.stripColor(e.getName().getString()).contains("SUPPLIES RECEIVED"))) {
                for (int i = 0; i < piles.size(); i++) {
                    var p = piles.get(i);
                    if (Math.hypot(s.getX() - (p.x() + 0.5), s.getZ() - (p.z() + 0.5)) < 3) {
                        completedPiles.add(i);
                    }
                }
            }

            if (inSuppliesPhase && piles.isEmpty()) {
                LOGGER.warn("[BZA] Pile waypoints enabled but no piles loaded, check pile_locations.json");
            }
        });

        RenderPhaseRegister.register(ctx -> {
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            var cfg = ModConfigManager.get().kuudra.phase1;
            if (!cfg.supplyPileWaypoints) return;
            if (!inSuppliesPhase) return;

            String noPre = NoPreAlert.getMissingPreName();
            int normalColor = cfg.supplyPileColor.getEffectiveColour().getRGB();
            for (int i = 0; i < piles.size(); i++) {
                if (completedPiles.contains(i)) continue; // 该位置已有补给，隐藏
                var pile = piles.get(i);
                boolean isNoPre = noPre != null && noPre.equalsIgnoreCase(pile.name());
                int color = isNoPre ? 0xFFFF5555 : normalColor; // ARGB，alpha 必须非 0
                float[] c = argbToFloats(color);

                // 信标光柱
                BeamRenderer.drawBeam(ctx, pile.x() + 0.5, pile.y(), pile.z() + 0.5,
                        PILE_BEACON_HEIGHT, 0.15f, color);

                // Box（正常深度测试）
                WorldRenderUtils.drawFilledBox(ctx,
                        pile.x(), pile.y(), pile.z(),
                        pile.x() + 1, pile.y() + 1, pile.z() + 1,
                        c[0], c[1], c[2], 0.2f, true);

                // Name label（独立颜色，默认纯白，不随光柱透明度）
                if (cfg.supplyPileNames) {
                    int nameColor = isNoPre ? 0xFFFF5555
                            : cfg.supplyPileNameColor.getEffectiveColourRGB();
                    WorldTextRenderer.renderString(ctx, pile.name(),
                            pile.x() + 0.5, pile.y() + 2.5, pile.z() + 0.5,
                            nameColor, 0.08f, true);
                }
            }
        });
    }

    /** 找到离坐标最近的 pile 名称（用于 No Pre 后提示去其他点位）。 */
    public static String findNearestPileName(double x, double z) {
        String best = null;
        double bestDist = Double.MAX_VALUE;
        for (var p : piles) {
            double d = Math.hypot(x - (p.x() + 0.5), z - (p.z() + 0.5));
            if (d < bestDist) {
                bestDist = d;
                best = p.name();
            }
        }
        return best;
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

    private static float[] argbToFloats(int argb) {
        return new float[] {
                ((argb >> 16) & 0xFF) / 255f,
                ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f,
                ((argb >> 24) & 0xFF) / 255f
        };
    }

    private static void loadConfig() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("babyzombieaddons");
        Path configFile = configDir.resolve("pile_locations.json");

        if (!Files.exists(configFile)) {
            try {
                Files.createDirectories(configDir);
                var defaultStream = KuudraPileWaypoints.class.getResourceAsStream(
                        "/default-config/babyzombieaddons/pile_locations.json");
                if (defaultStream != null) {
                    Files.copy(defaultStream, configFile);
                }
            } catch (IOException ignored) {}
        }

        try {
            String json = Files.readString(configFile);
            PileConfig root = new Gson().fromJson(json, PileConfig.class);
            if (root != null && root.piles != null && !root.piles.isEmpty()) {
                piles = new ArrayList<>();
                for (var p : root.piles) {
                    if (p.name != null && p.pos != null && p.pos.length == 3) {
                        piles.add(new Pile(p.name, p.pos[0], p.pos[1], p.pos[2]));
                    }
                }
                return;
            }
        } catch (Exception ignored) {}

        // 兜底：配置文件缺失/损坏时用内置默认
        try (var defaultStream = KuudraPileWaypoints.class.getResourceAsStream(
                "/default-config/babyzombieaddons/pile_locations.json")) {
            if (defaultStream != null) {
                String json = new String(defaultStream.readAllBytes(), StandardCharsets.UTF_8);
                PileConfig root = new Gson().fromJson(json, PileConfig.class);
                if (root != null && root.piles != null) {
                    piles = new ArrayList<>();
                    for (var p : root.piles) {
                        if (p.name != null && p.pos != null && p.pos.length == 3) {
                            piles.add(new Pile(p.name, p.pos[0], p.pos[1], p.pos[2]));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            piles = List.of();
        }
    }

    @SuppressWarnings("unused")
    private static class PileConfig {
        List<PileEntry> piles;
    }
    @SuppressWarnings("unused")
    private static class PileEntry {
        String name;
        double[] pos;
    }
}
