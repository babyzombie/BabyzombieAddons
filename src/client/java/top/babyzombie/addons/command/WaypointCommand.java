package top.babyzombie.addons.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.util.render.Waypoints;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

final class WaypointCommand {
    private WaypointCommand() {}

    static void register(
            com.mojang.brigadier.builder.ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        for (String alias : new String[]{"wp", "waypoint"}) {
            var cmd = literal(alias)
                    .executes(ctx -> showUsage(ctx.getSource()));
            cmd.then(buildAdd());
            cmd.then(buildDelete());
            parent.then(cmd);
        }
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<FabricClientCommandSource, ?>
            buildAdd() {
        var add = literal("add")
                .executes(ctx -> showUsage(ctx.getSource()));

        // /bza wp add here <名字> [秒数]
        var here = literal("here").executes(ctx -> showUsage(ctx.getSource()));
        here.then(nameArg(WaypointCommand::addHere));
        add.then(here);

        // /bza wp add looking <名字> [秒数]
        var looking = literal("looking").executes(ctx -> showUsage(ctx.getSource()));
        looking.then(nameArg(WaypointCommand::addLooking));
        add.then(looking);

        // /bza wp add ~ ~ ~ <名字> [秒数]  (等同于 here)
        add.then(literal("~").then(literal("~").then(literal("~")
                .then(nameArg(WaypointCommand::addHere)))));

        // /bza wp add <x> <y> <z> <名字> [秒数]
        add.then(argument("args", StringArgumentType.greedyString())
                .executes(ctx -> {
                    var player = Minecraft.getInstance().player;
                    if (player == null) return 1;
                    var pos = player.blockPosition();
                    String[] parts = StringArgumentType.getString(ctx, "args")
                            .split(" ");
                    if (parts.length < 4) {
                        ctx.getSource().sendError(Component.translatable(
                                "babyzombieaddons.waypoint.missing_args"));
                        return 1;
                    }
                    int x, y, z;
                    try {
                        x = resolveCoord(parts[0], pos.getX());
                        y = resolveCoord(parts[1], pos.getY());
                        z = resolveCoord(parts[2], pos.getZ());
                    } catch (NumberFormatException e) {
                        ctx.getSource().sendError(Component.translatable(
                                "babyzombieaddons.waypoint.invalid_coord"));
                        return 1;
                    }
                    String name;
                    int timeSec;
                    try {
                        timeSec = Integer.parseInt(parts[parts.length - 1]);
                        name = String.join(" ", java.util.Arrays.copyOfRange(
                                parts, 3, parts.length - 1));
                        if (name.isEmpty()) name = parts[parts.length - 1];
                    } catch (NumberFormatException e) {
                        timeSec = -1;
                        name = String.join(" ", java.util.Arrays.copyOfRange(
                                parts, 3, parts.length));
                    }
                    Waypoints.addWaypoint(name, x, y, z, timeSec, true);
                    return 1;
                }));

        return add;
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<FabricClientCommandSource, ?>
            nameArg(com.mojang.brigadier.Command<FabricClientCommandSource> cmd) {
        return argument("nameAndTime", StringArgumentType.greedyString()).executes(cmd);
    }

    private static int addHere(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx) {
        var player = Minecraft.getInstance().player;
        if (player == null) return 1;
        var pos = player.blockPosition();
        var p = parse(StringArgumentType.getString(ctx, "nameAndTime"));
        Waypoints.addWaypoint(p.name, pos.getX(), pos.getY(), pos.getZ(), p.timeSec, true);
        return 1;
    }

    private static int addLooking(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx) {
        var player = Minecraft.getInstance().player;
        if (player == null) return 1;
        var eyePos = player.getEyePosition();
        var lookVec = player.getViewVector(1.0f);
        var farPoint = eyePos.add(lookVec.scale(500.0));
        var hit = player.level().clip(new net.minecraft.world.level.ClipContext(
                eyePos, farPoint,
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            var pos = hit.getBlockPos();
            var p = parse(StringArgumentType.getString(ctx, "nameAndTime"));
            Waypoints.addWaypoint(p.name, pos.getX(), pos.getY(), pos.getZ(), p.timeSec, true);
        } else {
            ctx.getSource().sendFeedback(
                    Component.translatable("babyzombieaddons.sendcoords.no_target"));
        }
        return 1;
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<FabricClientCommandSource, ?>
            buildDelete() {
        return literal("delete")
                .executes(ctx -> showUsage(ctx.getSource()))
                .then(argument("name", StringArgumentType.greedyString())
                .executes(ctx -> {
                    Waypoints.deleteWaypoint(
                            StringArgumentType.getString(ctx, "name"), true);
                    return 1;
                }));
    }

    private static int resolveCoord(String arg, int playerCoord) {
        if (arg.startsWith("~")) {
            if (arg.length() == 1) return playerCoord;
            return playerCoord + Integer.parseInt(arg.substring(1));
        }
        return Integer.parseInt(arg);
    }

    private static int showUsage(FabricClientCommandSource src) {
        src.sendFeedback(Component.translatable("babyzombieaddons.waypoint.usage"));
        return 1;
    }

    private static NameAndTime parse(String raw) {
        String t = raw.trim();
        int lastSpace = t.lastIndexOf(' ');
        if (lastSpace > 0) {
            try {
                int time = Integer.parseInt(t.substring(lastSpace + 1));
                return new NameAndTime(t.substring(0, lastSpace), time);
            } catch (NumberFormatException ignored) {}
        }
        return new NameAndTime(t, -1);
    }

    private record NameAndTime(String name, int timeSec) {}
}
