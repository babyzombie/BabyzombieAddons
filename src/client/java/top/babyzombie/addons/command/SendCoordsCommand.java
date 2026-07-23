package top.babyzombie.addons.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import top.babyzombie.addons.util.ChatUtils;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

final class SendCoordsCommand {
    private SendCoordsCommand() {}

    static void register(
            com.mojang.brigadier.builder.ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("sc").executes(ctx -> send(ctx, "Self", null, null))
                .then(argument("extra", StringArgumentType.greedyString())
                        .executes(ctx -> parseArgs(ctx, "Self"))));
        parent.then(literal("sendcoords").executes(ctx -> send(ctx, "Self", null, null))
                .then(argument("extra", StringArgumentType.greedyString())
                        .executes(ctx -> parseArgs(ctx, "Self"))));
        parent.then(literal("la").executes(ctx -> send(ctx, "LookingAt", null, null))
                .then(argument("extra", StringArgumentType.greedyString())
                        .executes(ctx -> parseArgs(ctx, "LookingAt"))));
        parent.then(literal("lookingat").executes(ctx -> send(ctx, "LookingAt", null, null))
                .then(argument("extra", StringArgumentType.greedyString())
                        .executes(ctx -> parseArgs(ctx, "LookingAt"))));
    }

    private static int parseArgs(CommandContext<FabricClientCommandSource> ctx, String mode) {
        String raw = StringArgumentType.getString(ctx, "extra");
        String channel = null, suffix = null;
        String[] parts = raw.trim().split(" ", 2);
        if (channelToPrefix(parts[0]).isEmpty()) {
            suffix = raw.trim();
        } else {
            channel = parts[0].toLowerCase();
            if (parts.length > 1) suffix = parts[1];
        }
        return send(ctx, mode, channel, suffix);
    }

    static int send(CommandContext<FabricClientCommandSource> ctx,
                     String mode, String channel, String suffix) {
        var player = Minecraft.getInstance().player;
        if (player == null) return 1;
        var pos = player.blockPosition();
        if (mode.equals("LookingAt")) {
            var eyePos = player.getEyePosition();
            var lookVec = player.getViewVector(1.0F);
            var farPoint = eyePos.add(lookVec.scale(500.0));
            var hit = player.level().clip(new ClipContext(eyePos, farPoint,
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) {
                ctx.getSource().sendFeedback(
                        Component.translatable("babyzombieaddons.sendcoords.no_target"));
                return 1;
            }
            pos = hit.getBlockPos();
        }
        String msg = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
        if (suffix != null && !suffix.isEmpty()) msg += ", " + suffix;
        String prefix = channelToPrefix(channel);
        if (channel != null) ChatUtils.sendCommand(prefix + " " + msg);
        else ChatUtils.sendMessage(msg);
        return 1;
    }

    static String channelToPrefix(String ch) {
        if (ch == null) return "";
        return switch (ch.toLowerCase()) {
            case "ac", "pc", "gc", "oc", "cc" -> ch.toLowerCase();
            default -> "";
        };
    }
}
