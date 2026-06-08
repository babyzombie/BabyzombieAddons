package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import top.babyzombie.addons.util.RenderPhaseRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.DisplaySlot;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.BeamRenderer;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.WorldTextRenderer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class KuudraWaypoints {
    private KuudraWaypoints() {}

    private record Beam(double x, double y, double z, float r, float g, float b, float a, float h) {}

    private record TextData(String text, double x, double y, double z, int color) {}
    private static final Map<String, TextData> textEntries = new ConcurrentHashMap<>();
    private static final Set<String> seenKeys = ConcurrentHashMap.newKeySet();
    private static final List<Beam> beams = new ArrayList<>();

    public static void init() {
        RenderPhaseRegister.register(ctx -> {
            for (var t : textEntries.values())
                WorldTextRenderer.renderString(ctx, t.text, t.x, t.y, t.z, t.color, 0.05f, true);
            for (var b : beams) {
                BeamRenderer.drawBeam(ctx, b.x, b.y, b.z,
                    b.h, 0.15f, new Color(b.r, b.g, b.b, b.a).getRGB());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var cfg = ModConfigManager.get().kuudra;
            boolean anyOn = cfg.supplyBeacons || cfg.supplyDropoffBeacons
                    || cfg.ballistaProgressText || cfg.ballistaBuildBeacons
                    || cfg.fuelOrbBeacons;
            if (!anyOn) return;
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            if (client.player == null || client.player.tickCount % 20 != 0) return;

            beams.clear();
            seenKeys.clear();
            String phase = getScoreboardPhase(client);

            if ("Rescue supplies".equals(phase)) {
                if (cfg.supplyBeacons) {
                    float[] c = argbToFloats(cfg.supplyBeaconColor);
                    for (var g : client.player.level().getEntitiesOfClass(Giant.class,
                            new AABB(client.player.blockPosition()).inflate(64)))
                        beams.add(new Beam(g.getX() - 2.5, g.getY() + 9.5, g.getZ() + 3.0,
                                c[0], c[1], c[2], c[3], 20f));
                }
                if (cfg.supplyDropoffBeacons) {
                    float[] c = argbToFloats(cfg.supplyDropoffBeaconColor);
                    for (var s : client.player.level().getEntitiesOfClass(
                            net.minecraft.world.entity.decoration.ArmorStand.class,
                            new AABB(client.player.blockPosition()).inflate(64),
                            e -> ChatUtils.stripColor(e.getName().getString()).contains("BRING SUPPLY CHEST HERE"))) {
                        double x = s.getX(), z = s.getZ();
                        beams.add(new Beam(x, s.getY(), z, c[0], c[1], c[2], c[3], 20f));
                    }
                }
            } else if ("Protect Elle".equals(phase)) {
                if (cfg.ballistaBuildBeacons || cfg.ballistaProgressText) {
                    float[] bc = argbToFloats(cfg.ballistaBeaconColor);
                    for (var s : client.player.level().getEntitiesOfClass(
                            net.minecraft.world.entity.decoration.ArmorStand.class,
                            new AABB(client.player.blockPosition()).inflate(64),
                            e -> {
                                String name = ChatUtils.stripColor(e.getName().getString());
                                return name.startsWith("PROGRESS: ") && !name.endsWith("COMPLETE");
                            })) {
                        double x = s.getX(), y = s.getY(), z = s.getZ();
                        if (cfg.ballistaBuildBeacons)
                            beams.add(new Beam(x, y, z, bc[0], bc[1], bc[2], bc[3], 10f));
                        if (cfg.ballistaProgressText) {
                            String[] parts = ChatUtils.stripColor(s.getName().getString()).split(" ");
                            String key = "p2_" + s.getId(); seenKeys.add(key);
                            textEntries.put(key, new TextData(parts.length > 1 ? parts[parts.length - 1] : "",
                                    x + 1.5, y + 1.2, z + 1.5, cfg.ballistaTextColor));
                        }
                    }
                }
            } else {
                if (cfg.fuelOrbBeacons) {
                    float[] c = argbToFloats(cfg.fuelOrbBeaconColor);
                    for (var g : client.player.level().getEntitiesOfClass(Giant.class,
                            new AABB(client.player.blockPosition()).inflate(64)))
                        beams.add(new Beam(g.getX() - 2.5, g.getY() + 9.5, g.getZ() + 3.0,
                                c[0], c[1], c[2], c[3], 20f));
                }
            }

            textEntries.keySet().removeIf(k -> !seenKeys.contains(k));
        });
    }

    private static float[] argbToFloats(int argb) {
        return new float[] {
            ((argb >> 16) & 0xFF) / 255f,
            ((argb >> 8) & 0xFF) / 255f,
            (argb & 0xFF) / 255f,
            ((argb >> 24) & 0xFF) / 255f
        };
    }

    private static String getScoreboardPhase(Minecraft client) {
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
}
