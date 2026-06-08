package top.babyzombie.addons.util;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;

import java.util.function.Consumer;

/** Registers a world render callback that respects the configurable render phase. */
public final class RenderPhaseRegister {
    private RenderPhaseRegister() {}

    public static void register(Consumer<WorldRenderContext> renderer) {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            if (ModConfigManager.get().general.renderPhase == ModConfig.WorldRenderPhase.AFTER_ENTITIES)
                renderer.accept(ctx);
        });
        WorldRenderEvents.END_MAIN.register(ctx -> {
            if (ModConfigManager.get().general.renderPhase == ModConfig.WorldRenderPhase.END_MAIN)
                renderer.accept(ctx);
        });
    }
}
