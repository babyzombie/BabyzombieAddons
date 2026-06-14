package top.babyzombie.addons.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

final class RotationCommand {
    private RotationCommand() {}

    static void register(
            com.mojang.brigadier.builder.ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        for (String alias : new String[]{"yaw", "angle", "rotation"}) {
            parent.then(literal(alias)
                    .then(argument("yaw", StringArgumentType.word())
                    .then(argument("pitch", StringArgumentType.word())
                    .executes(RotationCommand::rotate))));
        }
    }

    private static int rotate(CommandContext<FabricClientCommandSource> ctx) {
        try {
            float yaw = Float.parseFloat(StringArgumentType.getString(ctx, "yaw"));
            float pitch = Float.parseFloat(StringArgumentType.getString(ctx, "pitch"));
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.setYRot(yaw % 360);
                player.setXRot(Mth.clamp(pitch, -90, 90));
                ctx.getSource().sendFeedback(Component.literal(
                        String.format("§6§l§aRotation: §by=%.1f p=%.1f",
                                player.getYRot(), player.getXRot())));
            }
        } catch (NumberFormatException e) {
            ctx.getSource().sendError(Component.literal("§cInvalid yaw/pitch"));
        }
        return 1;
    }
}
