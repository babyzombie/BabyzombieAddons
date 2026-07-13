package top.babyzombie.addons.module.autois;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.event.EntityRenderEvents;
import top.babyzombie.addons.event.ParticleRenderEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.Scheduler;

public final class AutoISModule {

    /** Seconds remaining before the next warp attempt is allowed. */
    private static int warpCooldown;

    private AutoISModule() {}

    public static void init() {
        BackWhenServerRestart.init();

        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "autois"),
                (context, tickCounter) -> {
                    if (!ModConfigManager.get().skyblock.autois.enabled) return;
                    var font = Minecraft.getInstance().font;
                    String text = Component.translatable("hud.babyzombieaddons.autois").getString();
                    if (warpCooldown > 0) text += " §7(" + warpCooldown + "s)";
                    int x = HudManager.x("AutoIS"), y = HudManager.y("AutoIS");
                    float s = HudManager.scale("AutoIS");
                    HudManager.drawScaled(context, font, text, x, y, s);
                });

        EntityRenderEvents.BEFORE_RENDER.register(entity -> {
            var cfg = ModConfigManager.get().skyblock;
            return cfg.autois.enabled && cfg.autois.hideEntities;
        });

        ParticleRenderEvents.BEFORE_ADD.register(particle -> {
            var cfg = ModConfigManager.get().skyblock;
            return cfg.autois.enabled && cfg.autois.hideEntities;
        });

        // Reset cooldown on world change so the timer waits before checking again
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((minecraft, level) -> {
            warpCooldown = ModConfigManager.get().skyblock.autois.delay;
        });

        // Persistent repeating timer — checks every second whether we need to warp
        Scheduler.scheduleRepeating(20, () -> {
            if (!ModConfigManager.get().skyblock.autois.enabled) return;

            if (warpCooldown > 0) {
                warpCooldown--;
                return;
            }

            var tracker = HypixelLocationTracker.getInstance();
            if (tracker.isInLimbo()) {
                ChatUtils.sendCommand("lobby");
                warpCooldown = ModConfigManager.get().skyblock.autois.delay;
                return;
            }
            if (!tracker.isOnHypixel()) return;

            if (tracker.isInSkyblock()) {
                var dest = ModConfigManager.get().skyblock.autois.dest;
                if (dest == ModConfig.AutoISDest.ISLAND && !tracker.isIn("Private Island")) {
                    ChatUtils.sendCommand("is");
                    warpCooldown = ModConfigManager.get().skyblock.autois.delay;
                } else if (dest == ModConfig.AutoISDest.GARDEN && !tracker.isIn("Garden")) {
                    ChatUtils.sendCommand("warp garden");
                    warpCooldown = ModConfigManager.get().skyblock.autois.delay;
                }
            } else {
                ChatUtils.sendCommand("play skyblock");
                warpCooldown = ModConfigManager.get().skyblock.autois.delay;
            }
        });
    }
}
