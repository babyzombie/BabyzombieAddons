package top.babyzombie.addons.module.hunting;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * 在 Safari 区域高亮特定实体：潜影贝（自定义颜色）、Hideyho NPC（淡蓝色）。
 */
public final class SafariEntitiesGlow {

    private static final String HIDEYHO_NAME = "Hideyho ";
    private static final int HIDEYHO_COLOR = 0xFF80D8FF;

    private SafariEntitiesGlow() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;
            if (!HypixelLocationTracker.getInstance().isInSafari()) return;

            var cfg = ModConfigManager.get().hunting;
            boolean glowShulker = cfg.safari.shulkerGlow;
            boolean glowHideyho = cfg.safari.hideyhoGlow;
            if (!glowShulker && !glowHideyho) return;

            for (var entity : client.level.entitiesForRendering()) {
                if (glowShulker && entity instanceof Shulker shulker) {
                    int argb = cfg.safari.shulkerGlowColor.getEffectiveColourRGB();
                    GlowController.setGlow(shulker, true, argb, true);
                }
                if (glowHideyho && entity instanceof Player player
                        && HIDEYHO_NAME.equals(player.getName().getString())) {
                    GlowController.setGlow(player, true, HIDEYHO_COLOR, true);
                }
            }
        });
    }
}
