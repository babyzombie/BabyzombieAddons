package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.DisplaySlot;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.BeaconBeamRenderer;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.WorldTextRenderer;
import top.babyzombie.addons.util.WorldTextRenderer.TextEntry;

import java.awt.Color;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class KuudraWaypoints {
    private KuudraWaypoints() {}

    private static final Map<String, TextEntry> textEntries = new ConcurrentHashMap<>();
    private static final Set<String> seenKeys = ConcurrentHashMap.newKeySet();
    private static boolean renderRegistered;

    public static void init() {
        if (!renderRegistered) {
            WorldRenderEvents.BEFORE_ENTITIES.register(ctx ->
                    WorldTextRenderer.render(ctx.matrices(), textEntries.values()));
            renderRegistered = true;
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().kuudra.waypoints) return;
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            if (client.player == null || client.player.tickCount % 20 != 0) return;

            BeaconBeamRenderer.clearAll();
            seenKeys.clear();
            String phase = getScoreboardPhase(client);

            if ("Rescue supplies".equals(phase)) {
                var giants = client.player.level().getEntitiesOfClass(Giant.class,
                        new AABB(client.player.blockPosition()).inflate(64));
                for (var g : giants)
                    BeaconBeamRenderer.addBeam(g.getX() - 2.5, g.getY() + 9.5, g.getZ() + 3.0,
                            Color.RED, 20f, 1000);
            } else if ("Protect Elle".equals(phase)) {
                var stands = client.player.level().getEntitiesOfClass(
                        net.minecraft.world.entity.decoration.ArmorStand.class,
                        new AABB(client.player.blockPosition()).inflate(64),
                        e -> {
                            String name = ChatUtils.stripColor(e.getName().getString());
                            return name.startsWith("PROGRESS: ") && !name.endsWith("COMPLETE");
                        });
                for (var s : stands) {
                    double x = s.getX() - 0.5, y = s.getY() + 0.8, z = s.getZ() - 0.5;
                    BeaconBeamRenderer.addBeam(x, y, z, Color.RED, 10f, 1000);
                    String[] parts = ChatUtils.stripColor(s.getName().getString()).split(" ");
                    String prog = parts.length > 1 ? parts[parts.length - 1] : "";
                    String key = "p2_" + s.getId();
                    seenKeys.add(key);
                    textEntries.put(key, new TextEntry(prog, x + 0.5, y + 1.5, z + 0.5, 0xFFFF55));
                }
            } else {
                var giants = client.player.level().getEntitiesOfClass(Giant.class,
                        new AABB(client.player.blockPosition()).inflate(64));
                for (var g : giants)
                    BeaconBeamRenderer.addBeam(g.getX() - 2.5, g.getY() + 9.5, g.getZ() + 3.0,
                            Color.RED, 20f, 1000);
            }

            var mites = client.player.level().getEntitiesOfClass(Endermite.class,
                    new AABB(client.player.blockPosition()).inflate(64),
                    e -> !e.isDeadOrDying());
            for (var m : mites)
                BeaconBeamRenderer.addBeam(m.getX(), m.getY(), m.getZ(),
                        new Color(1f, 0f, 0f, 0.7f), 10f, 1000);

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
