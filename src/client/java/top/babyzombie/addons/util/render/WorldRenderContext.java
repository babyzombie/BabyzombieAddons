package top.babyzombie.addons.util.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public final class WorldRenderContext {
    private final WorldState worldState;

    public WorldRenderContext(CameraRenderState cam) {
        this.worldState = new WorldState(cam);
    }

    public WorldState worldState() { return worldState; }
    // Legacy compatibility
    public WorldState levelState() { return worldState; }

    public static WorldRenderContext from(LevelRenderContext ctx) {
        return new WorldRenderContext(ctx.levelState().cameraRenderState);
    }

    public static final class WorldState {
        public final CameraRenderState cameraRenderState;
        WorldState(CameraRenderState s) { this.cameraRenderState = s; }
    }
}
