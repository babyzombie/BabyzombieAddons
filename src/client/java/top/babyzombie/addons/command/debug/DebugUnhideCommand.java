package top.babyzombie.addons.command.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.util.PlayerUtils;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class DebugUnhideCommand {
    private static final int DEFAULT_RANGE = 5;

    private DebugUnhideCommand() {}

    public static void register(ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("showinvisibleentity")
                .executes(ctx -> unhide(ctx.getSource(), DEFAULT_RANGE))
                .then(argument("range", IntegerArgumentType.integer(1))
                        .executes(ctx -> unhide(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "range")))));
    }

    private static int unhide(FabricClientCommandSource src, int range) {
        if (!PlayerUtils.isInSkyblock()) {
            src.sendFeedback(Component.translatable("babyzombieaddons.debug.unhide.skyblock_only"));
            return 0;
        }

        var player = src.getClient().player;
        if (player == null) return 0;

        var level = player.level();
        var box = new AABB(player.blockPosition()).inflate(range);
        var entities = level.getEntitiesOfClass(Entity.class, box, entity ->
                entity != player && entity.isInvisible());

        for (var entity : entities) {
            entity.setInvisible(false);
        }

        src.sendFeedback(Component.translatable("babyzombieaddons.debug.unhide.revealed",
                entities.size(), range));
        return 1;
    }
}
