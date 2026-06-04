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
import java.util.Arrays;
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
                String raw = team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString();
                if (!raw.contains("⧯")) continue;
                String[] parts = raw.split("⧯");
                for (int i = 0; i < parts.length && i < 6; i++) {
                    if (parts[i].endsWith("§7") || parts[i].endsWith("&7")) {
                        active.add(i);
                    }
                }
                break;
            }
        });

        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (!ModConfigManager.get().slayer.showEffigies) return;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock() || !"The Rift".equals(tracker.getMap())) return;
            if (active.isEmpty()) return;

            for (int idx : active) {
                if (idx >= EFFIGY_POS.length) continue;
                var pos = EFFIGY_POS[idx];
                BeaconStateInjector.addBeam(pos.getX(), pos.getY(), pos.getZ(),
                    new Color(255, 0, 0, 255));
                WorldRenderUtils.drawWireframeBox(ctx,
                    pos.getX(), pos.getY() + boxH, pos.getZ(),
                    pos.getX() + 1, pos.getY() + boxH + 1, pos.getZ() + 1,
                    1, 0, 0, 1, true, 4.0f);
            }
        });
    }

}
