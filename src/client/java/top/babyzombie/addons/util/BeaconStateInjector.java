package top.babyzombie.addons.util;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;

import java.util.ArrayList;
import java.util.List;

public final class BeaconStateInjector {

    private static final List<PendingBeam> pending = new ArrayList<>();

    public static void init() {
        WorldRenderEvents.END_EXTRACTION.register((WorldExtractionContext ctx) -> {
            if (pending.isEmpty()) return;
            var renderState = ctx.worldState();
            var client = Minecraft.getInstance();
            int animTime = Math.floorMod(client.level.getGameTime(), 40);
            float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            for (var p : pending) {
                var state = new BeaconRenderState();
                state.blockPos = new BlockPos((int) p.x, (int) p.y, (int) p.z);
                state.blockState = Blocks.BEACON.defaultBlockState();
                state.blockEntityType = BlockEntityType.BEACON;
                state.lightCoords = LightTexture.FULL_BRIGHT;
                state.breakProgress = null;
                state.animationTime = animTime + partialTick;
                state.sections.add(new BeaconRenderState.Section(p.color, (int) p.height));
                state.beamRadiusScale = 1.0f;
                renderState.blockEntityRenderStates.add(state);
            }
            pending.clear();
        });
    }

    public static void addBeam(double x, double y, double z, int color, float height) {
        pending.add(new PendingBeam(x, y, z, color, height));
    }

    public static void addBeam(double x, double y, double z, java.awt.Color c, float height) {
        pending.add(new PendingBeam(x, y, z, c.getRGB(), height));
    }

    private record PendingBeam(double x, double y, double z, int color, float height) {}
}
