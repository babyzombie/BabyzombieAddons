package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.scores.*;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows remaining undestroyed blood effigies in the Rift castle.
 */
public final class EffigyDisplay {
    private static final BlockPos[] EFFIGY_POS = {
        new BlockPos(150, 73, 95), new BlockPos(193, 87, 119),
        new BlockPos(235, 104, 147), new BlockPos(293, 90, 134),
        new BlockPos(262, 93, 94), new BlockPos(240, 123, 118)
    };
    private static final List<Integer> active = new ArrayList<>();
    private static int boxH;

    private EffigyDisplay() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().slayer.showEffigies) return;
            if (client.player == null) return;
            int t = client.player.tickCount % 20;
            if (t == 0) boxH = 6;
            else if (t == 5) boxH = 4;
            else if (t == 10) boxH = 2;
            else if (t == 15) boxH = 0;

            if (t != 0) return;

            active.clear();
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock() || !"The Rift".equals(tracker.getMap())) return;

            var level = client.player.level();
            if (level == null) return;
            Scoreboard sb = level.getScoreboard();
            Objective obj = sb.getDisplayObjective(DisplaySlot.BY_ID.apply(1));
            if (obj == null) return;

            for (ScoreHolder holder : sb.getTrackedPlayers()) {
                if (!sb.listPlayerScores(holder).containsKey(obj)) continue;
                PlayerTeam team = sb.getPlayersTeam(holder.getScoreboardName());
                if (team == null) continue;
                String rawText = team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString();
                // Match JS: RemoveEmoji, replace "Effigies: §", split by §, gray(§7)=destroyed
                String processed = ChatUtils.removeEmoji(rawText)
                    .replace("Effigies: §", "");
                if (processed.equals(rawText)) continue;
                String[] parts = processed.split("§");
                for (int i = 0; i < parts.length && i < 6; i++) {
                    if (!parts[i].equals("7")) active.add(i); // NOT destroyed = active
                }
                break;
            }
        });

        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (!ModConfigManager.get().slayer.showEffigies) return;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock() || !"The Rift".equals(tracker.getMap())) return;
            if (active.isEmpty()) return;
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            for (int idx : active) {
                if (idx >= EFFIGY_POS.length) continue;
                var pos = EFFIGY_POS[idx];
                BeaconBeamRenderer.render(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new Color(255, 0, 0, 255), BeaconBeamRenderer.DEFAULT_HEIGHT);

                double e = 0.02;
                WorldRenderUtils.drawBox(
                    pos.getX() - 0.5 - e, pos.getY() + boxH - 0.2 - e, pos.getZ() - 0.5 - e,
                    pos.getX() + 1.5 + e, pos.getY() + boxH + 0.6 + e, pos.getZ() + 1.5 + e,
                    1, 0, 0, 1);

                double dist = Math.sqrt(player.distanceToSqr(
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
                WorldTextRenderer.render(ctx.matrices(), List.of(new WorldTextRenderer.TextEntry(
                    "§e(§c" + (int)dist + "m§e)",
                    pos.getX() + 0.5, pos.getY() + boxH + 0.5, pos.getZ() + 0.5, 0xFFFF5555)));
            }
        });
    }
}
