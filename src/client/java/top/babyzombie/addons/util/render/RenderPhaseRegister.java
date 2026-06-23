package top.babyzombie.addons.util.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import top.babyzombie.addons.config.ModConfig.WorldRenderPhase;
import top.babyzombie.addons.config.ModConfigManager;

import java.util.function.Consumer;

public final class RenderPhaseRegister {
    private RenderPhaseRegister() {}

    public static void register(Consumer<WorldRenderContext> renderer) {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(ctx -> {
            if (ModConfigManager.get().general.renderPhase != WorldRenderPhase.AFTER_ENTITIES) return;
            renderer.accept(WorldRenderContext.from(ctx));
        });
        LevelRenderEvents.END_MAIN.register(ctx -> {
            if (ModConfigManager.get().general.renderPhase != WorldRenderPhase.END_MAIN) return;
            renderer.accept(WorldRenderContext.from(ctx));
        });
    }
}
