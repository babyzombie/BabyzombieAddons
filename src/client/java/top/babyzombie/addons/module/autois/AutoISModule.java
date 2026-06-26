package top.babyzombie.addons.module.autois;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
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
import top.babyzombie.addons.util.ServerTick;

public final class AutoISModule {

    private static boolean lobbyConfirmRegistered;
    private static long kickTime;

    private AutoISModule() {}

    public static void init() {
        BackWhenServerRestart.init();

        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "autois"),
                (context, tickCounter) -> {
                    if (!ModConfigManager.get().general.autois) return;
                    var font = Minecraft.getInstance().font;
                    int x = HudManager.x("AutoIS"), y = HudManager.y("AutoIS");
                    float s = HudManager.scale("AutoIS");
                    HudManager.drawScaled(context, font,
                            Component.translatable("hud.babyzombieaddons.autois").getString(),
                            x, y, s);
                });

        EntityRenderEvents.BEFORE_RENDER.register(entity -> {
            var cfg = ModConfigManager.get().general;
            return cfg.autois && cfg.hideEntities;
        });

        ParticleRenderEvents.BEFORE_ADD.register(particle -> {
            var cfg = ModConfigManager.get().general;
            return cfg.autois && cfg.hideEntities;
        });

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((minecraft, level) -> {
            if (!ModConfigManager.get().general.autois) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            int delayTicks = ModConfigManager.get().general.autoisDelay * 20;
            Scheduler.schedule(delayTicks, AutoISModule::doWarp);
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !ModConfigManager.get().general.autois) return;
            if (message.getString().equals("You were kicked while joining that server!")
                    || message.getString().equals("Oops! You are not on SkyBlock so we couldn't warp you!")) {
                kickTime = ServerTick.getTime();
                var mode = ModConfigManager.get().general.autoBackToSkyblock;
                if (mode != ModConfig.KickRecovery.OFF) {
                    lobbyConfirmRegistered = true;
                    ChatUtils.sendCommand("lobby");
                }
                if (mode == ModConfig.KickRecovery.LOBBY_AND_SKYBLOCK) {
                    Scheduler.schedule(1300, () -> {
                        if (ServerTick.getTime() - kickTime < 1300 * 50)
                            ChatUtils.sendCommand("play skyblock");
                    });
                }
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !lobbyConfirmRegistered) return;
            if (message.getString().equals("Are you sure? Type /lobby again if you really want to quit.")) {
                ChatUtils.sendCommand("lobby");
                lobbyConfirmRegistered = false;
            }
        });

        Scheduler.schedule(20, () -> lobbyConfirmRegistered = false);
    }

    private static void doWarp() {
        if (!ModConfigManager.get().general.autois) return;
        var tracker = HypixelLocationTracker.getInstance();
        if (tracker.isInSkyblock()) {
            var dest = ModConfigManager.get().general.autoisDest;
            if (dest == ModConfig.AutoISDest.ISLAND && !tracker.isIn("Private Island"))
                ChatUtils.sendCommand("is");
            else if (dest == ModConfig.AutoISDest.GARDEN && !tracker.isIn("Garden"))
                ChatUtils.sendCommand("warp garden");
        } else ChatUtils.sendCommand("play skyblock");
    }
}
