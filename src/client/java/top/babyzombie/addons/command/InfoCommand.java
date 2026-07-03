package top.babyzombie.addons.command;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

final class InfoCommand {
    private InfoCommand() {}

    static void register(
            com.mojang.brigadier.builder.ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("location").executes(ctx -> location(ctx.getSource())));
        parent.then(literal("scoreboard").executes(ctx -> scoreboard(ctx.getSource())));
    }

    private static int location(FabricClientCommandSource src) {
        var loc = HypixelLocationTracker.getInstance().getCurrentLocation();
        src.sendFeedback(Component.literal("§b" + loc.toString()));
        return 1;
    }

    private static int scoreboard(FabricClientCommandSource src) {
        var player = Minecraft.getInstance().player;
        if (player == null) return 1;
        var level = player.level();
        if (level == null) return 1;
        var sb = level.getScoreboard();
        var obj = sb.getDisplayObjective(net.minecraft.world.scores.DisplaySlot.BY_ID.apply(1));
        if (obj == null) {
            src.sendFeedback(Component.literal("§cNo scoreboard found"));
            return 1;
        }
        var title = obj.getDisplayName().getString();
        var sb2 = new StringBuilder();
        sb2.append("§6§lScoreboard: §r").append(title).append('\n');
        var lines = new java.util.TreeMap<Integer, String>(java.util.Collections.reverseOrder());
        for (var holder : sb.getTrackedPlayers()) {
            if (!sb.listPlayerScores(holder).containsKey(obj)) continue;
            var team = sb.getPlayersTeam(holder.getScoreboardName());
            if (team == null) continue;
            String text = team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString();
            int score = sb.listPlayerScores(holder).getInt(obj);
            lines.put(score, text);
        }
        int i = 0;
        for (var e : lines.entrySet()) {
            sb2.append(i++).append(" §7[§r").append(e.getValue()).append("§7]§r\n");
        }
        src.sendFeedback(Component.literal(sb2.toString()));
        return 1;
    }
}
