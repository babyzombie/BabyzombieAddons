package top.babyzombie.addons;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.playcmd.PlayCmdModule;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class BabyzombieAddonsCommand {
    private BabyzombieAddonsCommand() {}

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var bza = literal("bza").executes(BabyzombieAddonsCommand::settings)
                    .then(literal("s").executes(BabyzombieAddonsCommand::settings))
                    .then(literal("settings").executes(BabyzombieAddonsCommand::settings))
                    .then(literal("play").executes(BabyzombieAddonsCommand::play))
                    .then(literal("sc").executes(ctx -> sendcoords(ctx, "Self", null, null))
                            .then(argument("extra", StringArgumentType.greedyString())
                                    .executes(ctx -> parseSendcoordsArgs(ctx, "Self"))))
                    .then(literal("sendcoords").executes(ctx -> sendcoords(ctx, "Self", null, null))
                            .then(argument("extra", StringArgumentType.greedyString())
                                    .executes(ctx -> parseSendcoordsArgs(ctx, "Self"))))
                    .then(literal("la").executes(ctx -> sendcoords(ctx, "LookingAt", null, null))
                            .then(argument("extra", StringArgumentType.greedyString())
                                    .executes(ctx -> parseSendcoordsArgs(ctx, "LookingAt"))))
                    .then(literal("lookingat").executes(ctx -> sendcoords(ctx, "LookingAt", null, null))
                            .then(argument("extra", StringArgumentType.greedyString())
                                    .executes(ctx -> parseSendcoordsArgs(ctx, "LookingAt"))))
                    .then(literal("location").executes(BabyzombieAddonsCommand::location))
                    .then(literal("scoreboard").executes(BabyzombieAddonsCommand::scoreboard))
                    .then(literal("yaw").then(argument("yaw", StringArgumentType.word())
                            .then(argument("pitch", StringArgumentType.word())
                            .executes(BabyzombieAddonsCommand::rotation))))
                    .then(literal("angle").then(argument("yaw", StringArgumentType.word())
                            .then(argument("pitch", StringArgumentType.word())
                            .executes(BabyzombieAddonsCommand::rotation))))
                    .then(literal("rotation").then(argument("yaw", StringArgumentType.word())
                            .then(argument("pitch", StringArgumentType.word())
                            .executes(BabyzombieAddonsCommand::rotation))))
                    .then(literal("l").executes(ctx -> { ChatUtils.sendCommand("limbo"); return 1; }))
                    ;


            dispatcher.register(bza);
            dispatcher.register(literal("babyzombieaddons").executes(BabyzombieAddonsCommand::settings)
                    .then(literal("settings").executes(BabyzombieAddonsCommand::settings)));
        });
    }

    private static int settings(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().setScreen(ModConfigManager.createGUI(null)));
        return 1;
    }

    private static int play(CommandContext<FabricClientCommandSource> ctx) {
        PlayCmdModule.openGUI();
        return 1;
    }

    private static int parseSendcoordsArgs(CommandContext<FabricClientCommandSource> ctx, String mode) {
        String raw = StringArgumentType.getString(ctx, "extra");
        String channel = null, suffix = null;
        String[] parts = raw.trim().split(" ", 2);
        if (channelToPrefix(parts[0]).isEmpty()) {
            suffix = raw.trim();
        } else {
            channel = parts[0].toLowerCase();
            if (parts.length > 1) suffix = parts[1];
        }
        return sendcoords(ctx, mode, channel, suffix);
    }

    private static int sendcoords(CommandContext<FabricClientCommandSource> ctx, String mode, String channel, String suffix) {
        var player = Minecraft.getInstance().player;
        if (player == null) return 1;
        var pos = player.blockPosition();
        if (mode.equals("LookingAt")) {
            var hit = player.pick(20, 0, false);
            if (hit instanceof net.minecraft.world.phys.BlockHitResult bhit
                    && hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS)
                pos = bhit.getBlockPos();
        }
        String msg = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
        if (suffix != null && !suffix.isEmpty()) msg += ", " + suffix;
        String prefix = channelToPrefix(channel);
        ctx.getSource().sendFeedback(Component.literal("§6§l§b" + mode + (channel != null ? " " + prefix : "") + " §7: §a" + msg));
        if (channel != null) ChatUtils.sendCommand(prefix + " " + msg);
        else ChatUtils.sendMessage(msg);
        return 1;
    }

    private static String channelToPrefix(String ch) {
        if (ch == null) return "";
        return switch (ch.toLowerCase()) {
            case "ac" -> "ac";
            case "pc" -> "pc";
            case "gc" -> "gc";
            case "oc" -> "oc";
            case "cc" -> "cc";
                default -> "";
        };
    }

    private static int location(CommandContext<FabricClientCommandSource> ctx) {
        var loc = HypixelLocationTracker.getInstance().getCurrentLocation();
        ctx.getSource().sendFeedback(Component.literal("§b" + loc.toString()));
        return 1;
    }

    private static int scoreboard(CommandContext<FabricClientCommandSource> ctx) {
        var player = Minecraft.getInstance().player;
        if (player == null) return 1;
        var level = player.level();
        if (level == null) return 1;
        var sb = level.getScoreboard();
        var obj = sb.getDisplayObjective(net.minecraft.world.scores.DisplaySlot.BY_ID.apply(1));
        if (obj == null) {
            ctx.getSource().sendFeedback(Component.literal("§cNo scoreboard found"));
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
            int score = sb.listPlayerScores(holder).get(obj);
            lines.put(score, text);
        }
        int i = 0;
        for (var e : lines.entrySet()) {
            sb2.append(i++).append(" §7[§r").append(e.getValue()).append("§7]§r\n");
        }
        String result = sb2.toString();
        ctx.getSource().sendFeedback(Component.literal(result));
        return 1;
    }

    private static int rotation(CommandContext<FabricClientCommandSource> ctx) {
        try {
            float yaw = Float.parseFloat(StringArgumentType.getString(ctx, "yaw"));
            float pitch = Float.parseFloat(StringArgumentType.getString(ctx, "pitch"));
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.setYRot(yaw % 360);
                player.setXRot(Mth.clamp(pitch, -90, 90));
                ctx.getSource().sendFeedback(Component.literal(
                        String.format("§6§l§aRotation: §by=%.1f p=%.1f", player.getYRot(), player.getXRot())));
            }
        } catch (NumberFormatException e) {
            ctx.getSource().sendError(Component.literal("§cInvalid yaw/pitch"));
        }
        return 1;
    }
}
