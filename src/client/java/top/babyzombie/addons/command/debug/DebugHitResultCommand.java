package top.babyzombie.addons.command.debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import top.babyzombie.addons.util.render.WorldRenderContext;
import top.babyzombie.addons.util.render.WorldRenderUtils;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class DebugHitResultCommand {
    private static boolean showHitResult;
    private static final float BOX_ALPHA = 0.3f;

    private DebugHitResultCommand() {}

    // ==================== Render hook ====================

    static {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(ctx -> {
            if (!showHitResult) return;
            var mc = Minecraft.getInstance();
            var hit = mc.hitResult;
            if (hit == null) return;

            var renderCtx = WorldRenderContext.from(ctx);
            final float r = 0.0f, g = 1.0f, b = 0.0f;

            switch (hit.getType()) {
                case ENTITY -> {
                    var entity = ((EntityHitResult) hit).getEntity();
                    if (entity == null || entity.isRemoved()) return;
                    var aabb = entity.getBoundingBox();
                    WorldRenderUtils.drawFilledBox(renderCtx,
                            aabb.minX, aabb.minY, aabb.minZ,
                            aabb.maxX, aabb.maxY, aabb.maxZ,
                            r, g, b, BOX_ALPHA, true);
                }
                case BLOCK -> {
                    var blockHit = (BlockHitResult) hit;
                    var pos = blockHit.getBlockPos();
                    var level = mc.level;
                    if (level == null) return;
                    var state = level.getBlockState(pos);
                    if (state.isAir()) return;
                    var shape = state.getShape(level, pos);
                    if (shape.isEmpty()) return;
                    for (var box : shape.toAabbs()) {
                        WorldRenderUtils.drawFilledBox(renderCtx,
                                box.minX + pos.getX() - 0.01, box.minY + pos.getY() - 0.01, box.minZ + pos.getZ() - 0.01,
                                box.maxX + pos.getX() + 0.01, box.maxY + pos.getY() + 0.01, box.maxZ + pos.getZ() + 0.01,
                                r, g, b, BOX_ALPHA, true);
                    }
                }
            }
        });
    }

    // ==================== Command registration ====================

    public static void register(ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("gethitresult").executes(ctx -> getHitResult(ctx.getSource())));
        parent.then(literal("showhitresult").executes(ctx -> toggleShowHitResult(ctx.getSource())));
    }

    // ==================== gethitresult ====================

    private static int getHitResult(FabricClientCommandSource src) {
        var mc = Minecraft.getInstance();
        var hit = mc.hitResult;

        if (hit == null) {
            src.sendFeedback(Component.literal("§cNo hit result"));
            return 1;
        }

        switch (hit.getType()) {
            case ENTITY -> {
                var entity = ((EntityHitResult) hit).getEntity();
                DebugEntityCommand.dumpEntity(src, entity);
            }
            case BLOCK -> {
                dumpBlock(src, (BlockHitResult) hit);
            }
            case MISS -> {
                src.sendFeedback(Component.literal("§6§l=== HitResult ===\n§7Type: §fMISS\n§7§oNo target hit"));
            }
        }

        return 1;
    }

    private static void dumpBlock(FabricClientCommandSource src, BlockHitResult hit) {
        var mc = Minecraft.getInstance();
        var pos = hit.getBlockPos();
        var level = mc.level;
        if (level == null) return;

        var state = level.getBlockState(pos);
        var block = state.getBlock();
        var blockKey = BuiltInRegistries.BLOCK.getKey(block).toString();

        var sb = new StringBuilder();
        sb.append("§6§l=== HitResult ===\n");
        sb.append("§7Type: §fBLOCK\n");
        sb.append("§7Pos: §f").append(pos.getX()).append(", ").append(pos.getY()).append(", ").append(pos.getZ()).append("\n");
        sb.append("§7Block: §f").append(blockKey).append("\n");
        sb.append("§7Direction: §f").append(hit.getDirection()).append("\n");
        sb.append("§7Inside: §f").append(hit.isInside()).append("\n");

        // Block state properties
        var properties = state.getValues().toList();
        if (!properties.isEmpty()) {
            sb.append("\n§6§l--- Properties ---\n");
            for (var pv : properties) {
                sb.append("§7").append(pv.property().getName())
                        .append(" = §a").append(pv.value().toString()).append("\n");
            }
        }

        // Block entity info
        var be = level.getBlockEntity(pos);
        if (be != null) {
            sb.append("\n§6§l--- BlockEntity ---\n");
            sb.append("§7Type: §f").append(be.getClass().getSimpleName()).append("\n");
            try {
                var tag = be.saveWithoutMetadata(level.registryAccess());
                sb.append("§7NBT: §f").append(tag.toString()).append("\n");
            } catch (Exception ignored) {
                sb.append("§7§o(NBT unavailable)\n");
            }
        }

        // Block hit shape (outline)
        var shape = state.getShape(level, pos);
        sb.append("\n§6§l--- Hit Shape ---\n");
        if (shape.isEmpty()) {
            sb.append("§7(empty)\n");
        } else {
            for (var box : shape.toAabbs()) {
                sb.append("§7  §f")
                        .append(String.format("%.2f %.2f %.2f",
                                box.minX + pos.getX(), box.minY + pos.getY(), box.minZ + pos.getZ()))
                        .append(" §7→ §f")
                        .append(String.format("%.2f %.2f %.2f",
                                box.maxX + pos.getX(), box.maxY + pos.getY(), box.maxZ + pos.getZ()))
                        .append("\n");
            }
        }

        // Item representation
        var itemKey = BuiltInRegistries.ITEM.getKey(block.asItem());
        sb.append("\n§7Item: §f").append(itemKey);

        src.sendFeedback(Component.literal(sb.toString()));
    }

    // ==================== showhitresult ====================

    private static int toggleShowHitResult(FabricClientCommandSource src) {
        showHitResult = !showHitResult;
        src.sendFeedback(Component.translatable(
                showHitResult
                        ? "babyzombieaddons.debug.hitresult.show_on"
                        : "babyzombieaddons.debug.hitresult.show_off"));
        return 1;
    }
}
