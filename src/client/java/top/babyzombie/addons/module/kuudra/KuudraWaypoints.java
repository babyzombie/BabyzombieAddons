package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.DisplaySlot;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.BeaconStateInjector;
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
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            for (var t : textEntries.values())
                WorldTextRenderer.renderString(ctx, t.text, t.x, t.y, t.z, t.color, 0.025f, true);
            for (var b : beams) {
                BeaconStateInjector.addBeam(b.x, b.y, b.z,
                    new Color(b.r, b.g, b.b, b.a), b.h);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().kuudra.waypoints) return;
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            if (client.player == null || client.player.tickCount % 20 != 0) return;

            beams.clear();
            seenKeys.clear();
            String phase = getScoreboardPhase(client);

            if ("Rescue supplies".equals(phase)) {
                for (var g : client.player.level().getEntitiesOfClass(Giant.class,
                        new AABB(client.player.blockPosition()).inflate(64)))
                    beams.add(new Beam(g.getX()-2.5, g.getY()+9.5, g.getZ()+3.0, 0,1,0,1,20f));
                for (var s : client.player.level().getEntitiesOfClass(
                        net.minecraft.world.entity.decoration.ArmorStand.class,
                        new AABB(client.player.blockPosition()).inflate(64),
                        e -> ChatUtils.stripColor(e.getName().getString()).contains("BRING SUPPLY CHEST HERE"))) {
                    double x = s.getX()-1.5, y = s.getY(), z = s.getZ()-1.5;
                    beams.add(new Beam(x, y, z, 1, 1, 0, 1, 20f));
                    String key = "p1_supply_" + s.getId(); seenKeys.add(key);
                    double dist = Math.sqrt(client.player.distanceToSqr(x, y, z));
                    textEntries.put(key, new TextData("§e(§6" + (int)dist + "m§e)", x+0.5, y+1, z+0.5, 0xFFFFFF55));
                }
            } else if ("Protect Elle".equals(phase)) {
                for (var s : client.player.level().getEntitiesOfClass(
                        net.minecraft.world.entity.decoration.ArmorStand.class,
                        new AABB(client.player.blockPosition()).inflate(64),
                        e -> {
                            String name = ChatUtils.stripColor(e.getName().getString());
                            return name.startsWith("PROGRESS: ") && !name.endsWith("COMPLETE");
                        })) {
                    double x = s.getX()-1.5, y = s.getY()+0.8, z = s.getZ()-1.5;
                    beams.add(new Beam(x,y,z,0.3f,0.5f,1,1,10f));
                    String[] parts = ChatUtils.stripColor(s.getName().getString()).split(" ");
                    String key = "p2_" + s.getId(); seenKeys.add(key);
                    textEntries.put(key, new TextData(parts.length>1 ? parts[parts.length-1] : "", x+0.5, y+1.5, z+0.5, 0xFFFFFF55));
                }
            } else {
                for (var g : client.player.level().getEntitiesOfClass(Giant.class,
                        new AABB(client.player.blockPosition()).inflate(64)))
                    beams.add(new Beam(g.getX()-2.5, g.getY()+9.5, g.getZ()+3.0, 1,0,0,1,20f));
            }

            for (var m : client.player.level().getEntitiesOfClass(Endermite.class,
                    new AABB(client.player.blockPosition()).inflate(64), e -> !e.isDeadOrDying()))
                beams.add(new Beam(m.getX(),m.getY(),m.getZ(),0,1,0,0.7f,10f));

            textEntries.keySet().removeIf(k -> !seenKeys.contains(k));
        });
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
