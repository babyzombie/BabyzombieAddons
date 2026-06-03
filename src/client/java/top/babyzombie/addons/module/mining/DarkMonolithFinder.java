package top.babyzombie.addons.module.mining;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.BeaconBeamRenderer;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.WorldRenderUtils;
import top.babyzombie.addons.util.WorldTextRenderer;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.WorldTextRenderer.TextEntry;

import java.awt.Color;

public final class DarkMonolithFinder {
    private static final BlockPos[] MONOLITH_POSITIONS = {
            new BlockPos(-94, 201, -30), new BlockPos(-91, 221, -53),
            new BlockPos(-64, 206, -63), new BlockPos(-15, 236, -92),
            new BlockPos(-10, 162, 109), new BlockPos(1, 170, 0),
            new BlockPos(1, 183, 25), new BlockPos(49, 202, -162),
            new BlockPos(56, 214, -25), new BlockPos(61, 204, 181),
            new BlockPos(77, 160, 162), new BlockPos(91, 187, 131),
            new BlockPos(128, 187, 58), new BlockPos(150, 196, 190)
    };

    private static final List<BlockPos> shown = new ArrayList<>();
    private static long resetTimer;

    private DarkMonolithFinder() {}

    public static void init() {
        for (var p : MONOLITH_POSITIONS) shown.add(p);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().mining.darkMonolithFinder) return;
            if (!isInDwarvenMines()) return;
            if (client.player == null || client.player.tickCount % 10 != 0) return;

            var playerPos = client.player.position();

            if (shown.isEmpty()) {
                if (resetTimer == 0) resetTimer = ServerTick.getTime() + 30_000;
                else if (ServerTick.getTime() > resetTimer) {
                    for (var p : MONOLITH_POSITIONS) shown.add(p);
                    resetTimer = 0;
                }
                return;
            }

            // Check if player is looking at a monolith with dragon egg
            var hit = client.hitResult;
            if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                var hitPos = ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos();
                var level = client.player.level();
                for (var pos : MONOLITH_POSITIONS) {
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            var checkPos = new BlockPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                            if (checkPos.distSqr(hitPos) < 25) {
                                if (level.getBlockState(checkPos).is(Blocks.DRAGON_EGG)) {
                                    shown.clear();
                                    shown.add(pos);
                                    resetTimer = 0;
                                    return;
                                }
                                if (!level.getBlockState(new BlockPos(pos.getX() + dx, pos.getY() - 1, pos.getZ() + dz)).isAir()) {
                                    shown.remove(pos);
                                }
                            }
                        }
                    }
                }
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.darkMonolithFinder) return;
            if (!isInDwarvenMines()) return;
            if (ChatUtils.stripColor(message.getString()).startsWith("MONOLITH! You found a mysterious Dark Monolith")) {
                shown.clear();
            }
        });

        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (!ModConfigManager.get().mining.darkMonolithFinder) return;
            if (!isInDwarvenMines() || shown.isEmpty()) return;

            var player = Minecraft.getInstance().player;
            if (player == null) return;
            var camPos = player.getEyePosition();
            var entries = new ArrayList<TextEntry>();
            for (var pos : shown) {
                BeaconBeamRenderer.render(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        new Color(102, 0, 204, 128), 20f);
                double e = 0.02;
                WorldRenderUtils.drawBoxXray(
                    pos.getX() - e, pos.getY() - e, pos.getZ() - e,
                    pos.getX() + 1 + e, pos.getY() + 3 + e, pos.getZ() + 1 + e,
                    0.4f, 0, 0.8f, 0.5f);
                double dist = camPos.distanceTo(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
                String label = (shown.size() == 1 ? "§5§l* " : "§5") + "Dark Monolith §6(" + (int) dist + "m)";
                entries.add(new TextEntry(label, pos.getX() + 0.5, pos.getY() + 3.5, pos.getZ() + 0.5, 0xAA00CC));
            }
            WorldTextRenderer.render(ctx.matrices(), entries);
        });
    }

    private static boolean isInDwarvenMines() {
        var t = HypixelLocationTracker.getInstance();
        return t.isInSkyblock() && "Dwarven Mines".equals(t.getMap());
    }
}
