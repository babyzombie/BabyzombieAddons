package top.babyzombie.addons.module.garden;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.HypixelLocationTracker;

/**
 * Reads yaw/pitch from garden signs and sets player rotation.
 * Triggered by right-clicking a sign in the Garden.
 */
public final class SignAutoRotate {
    private SignAutoRotate() {}

    public static void init() {
        if (!ModConfigManager.get().garden.signAutoRotate) return;

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() || !HypixelLocationTracker.getInstance().isInSkyblock())
                return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            if (!world.getBlockState(pos).is(Blocks.OAK_WALL_SIGN)
                && !world.getBlockState(pos).is(Blocks.OAK_SIGN)
                && !world.getBlockState(pos).is(Blocks.BIRCH_WALL_SIGN)
                && !world.getBlockState(pos).is(Blocks.BIRCH_SIGN)
                && !world.getBlockState(pos).is(Blocks.SPRUCE_WALL_SIGN)
                && !world.getBlockState(pos).is(Blocks.SPRUCE_SIGN)
                && !world.getBlockState(pos).is(Blocks.JUNGLE_WALL_SIGN)
                && !world.getBlockState(pos).is(Blocks.JUNGLE_SIGN)
                && !world.getBlockState(pos).is(Blocks.ACACIA_WALL_SIGN)
                && !world.getBlockState(pos).is(Blocks.ACACIA_SIGN)
                && !world.getBlockState(pos).is(Blocks.DARK_OAK_WALL_SIGN)
                && !world.getBlockState(pos).is(Blocks.DARK_OAK_SIGN)
                && !world.getBlockState(pos).is(Blocks.MANGROVE_WALL_SIGN)
                && !world.getBlockState(pos).is(Blocks.MANGROVE_SIGN)
                && !world.getBlockState(pos).is(Blocks.CHERRY_WALL_SIGN)
                && !world.getBlockState(pos).is(Blocks.CHERRY_SIGN)
                && !world.getBlockState(pos).is(Blocks.BAMBOO_WALL_SIGN)
                && !world.getBlockState(pos).is(Blocks.BAMBOO_SIGN))
                return InteractionResult.PASS;

            // Read sign text lines and parse yaw/pitch
            BlockEntity be = world.getBlockEntity(pos);
            if (!(be instanceof SignBlockEntity sign)) return InteractionResult.PASS;

            var frontText = sign.getFrontText();
            String line1 = frontText.getMessage(0, false).getString().trim();
            String line2 = frontText.getMessage(1, false).getString().trim();
            try {

                // Parse yaw/pitch from sign lines (format: "yaw:XX pitch:YY")
                float yaw = Float.NaN, pitch = Float.NaN;
                if (line1.toLowerCase().contains("yaw")) {
                    yaw = Float.parseFloat(line1.replaceAll("[^0-9.\\-]", ""));
                }
                if (line2.toLowerCase().contains("pitch")) {
                    pitch = Float.parseFloat(line2.replaceAll("[^0-9.\\-]", ""));
                }
                // Try swapped lines
                if (line2.toLowerCase().contains("yaw") && Float.isNaN(yaw)) {
                    yaw = Float.parseFloat(line2.replaceAll("[^0-9.\\-]", ""));
                }
                if (line1.toLowerCase().contains("pitch") && Float.isNaN(pitch)) {
                    pitch = Float.parseFloat(line1.replaceAll("[^0-9.\\-]", ""));
                }

                if (!Float.isNaN(yaw) && !Float.isNaN(pitch)) {
                    var clientPlayer = Minecraft.getInstance().player;
                    if (clientPlayer != null) {
                        clientPlayer.setYRot(yaw);
                        clientPlayer.setXRot(pitch);
                    }
                }
            } catch (NumberFormatException ignored) {}

            return InteractionResult.PASS;
        });
    }
}
