package top.babyzombie.addons.util.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import java.util.function.Consumer;

public final class RenderPhaseRegister {
    private RenderPhaseRegister() {}

    public static void register(Consumer<WorldRenderContext> renderer) {
        LevelRenderEvents.END_MAIN.register(ctx ->
            renderer.accept(new WorldRenderContext(ctx.levelState().cameraRenderState)));
    }
}
