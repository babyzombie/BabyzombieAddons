package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.BeaconBeamRenderer;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.WorldRenderUtils;
import top.babyzombie.addons.util.WorldTextRenderer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows remaining undestroyed blood effigies in the Rift castle.
 */
public final class EffigyDisplay {
    // 6 fixed effigy positions in Stillgore Château
    private static final BlockPos[] EFFIGY_POS = {
        new BlockPos(150, 73, 95),
        new BlockPos(193, 87, 119),
        new BlockPos(235, 104, 147),
        new BlockPos(293, 90, 134),
        new BlockPos(262, 93, 94),
        new BlockPos(240, 123, 118)
    };

    // Indices of active (not destroyed) effigies
    private static final List<Integer> activeEffigies = new ArrayList<>();
    private static int boxH = 0;

    private EffigyDisplay() {}

    public static void init() {
        // Animation cycle: 6 → 4 → 2 → 0, refreshes every second
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().slayer.showEffigies) return;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock() || !"The Rift".equals(tracker.getMap())) return;

            if (client.player != null && client.player.tickCount % 20 == 0) {
                boxH = 6;
            } else if (client.player != null && client.player.tickCount % 20 == 5) {
                boxH = 4;
            } else if (client.player != null && client.player.tickCount % 20 == 10) {
                boxH = 2;
            } else if (client.player != null && client.player.tickCount % 20 == 15) {
                boxH = 0;
            }
        });

        // Parse scoreboard for effigy status
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().slayer.showEffigies) return;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock() || !"The Rift".equals(tracker.getMap())) return;
            if (client.player == null || client.player.tickCount % 10 != 0) return;

            activeEffigies.clear();
            var level = client.player.level();
            if (level == null) return;
            Scoreboard sb = level.getScoreboard();
            Objective obj = sb.getDisplayObjective(DisplaySlot.BY_ID.apply(1));
            if (obj == null) return;

            for (ScoreHolder holder : sb.getTrackedPlayers()) {
                if (!sb.listPlayerScores(holder).containsKey(obj)) continue;
                PlayerTeam team = sb.getPlayersTeam(holder.getScoreboardName());
                if (team == null) continue;
                String text = team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString();
                String plain = ChatUtils.removeEmoji(ChatUtils.stripColor(text.toString()));

                if (!plain.startsWith("Effigies: ") || !text.contains("⧯")) continue;

                // Parse color codes: "Effigies: §a⧯§7⧯§a⧯..." → each §x is a pillar's status
                String after = plain.substring("Effigies: ".length());
                String[] parts = text.substring(text.indexOf('§') + 1).split("§");
                for (int i = 0; i < parts.length && i < 6; i++) {
                    // If color code is NOT §7 (gray), the pillar is still active
                    if (!parts[i].startsWith("7")) {
                        activeEffigies.add(i);
                    }
                }
                break;
            }
        });

        // Render beams and boxes for active effigies
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (!ModConfigManager.get().slayer.showEffigies) return;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock() || !"The Rift".equals(tracker.getMap())) return;
            if (activeEffigies.isEmpty()) return;

            var player = Minecraft.getInstance().player;
            if (player == null) return;

            for (int idx : activeEffigies) {
                if (idx >= EFFIGY_POS.length) continue;
                var pos = EFFIGY_POS[idx];

                // Beacon beam
                BeaconBeamRenderer.setBeam("effigy_" + idx,
                    pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    new Color(255, 0, 0, 255), BeaconBeamRenderer.DEFAULT_HEIGHT);

                // Wireframe box
                WorldRenderUtils.drawBox(
                    pos.getX() - 0.5, pos.getY() + boxH - 0.2, pos.getZ() - 0.5,
                    pos.getX() + 1.5, pos.getY() + boxH + 0.6, pos.getZ() + 1.5,
                    1, 0, 0, 1);

                // Distance text
                double dist = Math.sqrt(player.distanceToSqr(
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
                var entries = List.of(new WorldTextRenderer.TextEntry(
                    "§e(§c" + (int)dist + "m§e)",
                    pos.getX() + 0.5, pos.getY() + boxH + 0.5, pos.getZ() + 0.5,
                    0xFFFF5555));
                WorldTextRenderer.render(ctx.matrices(), entries);
            }
        });
    }
}
