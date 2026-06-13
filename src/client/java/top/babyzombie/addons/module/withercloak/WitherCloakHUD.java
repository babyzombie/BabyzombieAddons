package top.babyzombie.addons.module.withercloak;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

public final class WitherCloakHUD {
    private WitherCloakHUD() {}

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "wither_cloak_hud"),
                (context, tickCounter) -> {
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            render(context);
        });
    }

    private static void render(GuiGraphicsExtractor gui) {
        var font = Minecraft.getInstance().font;
        long now = ServerTick.getTime();

        // Wither Cloak
        if (HudManager.shouldShow("WitherCloakTimer")) {
            int x = HudManager.x("WitherCloakTimer"), y = HudManager.y("WitherCloakTimer");
            float s = HudManager.scale("WitherCloakTimer");
            if (now - WitherCloakTimer.cooldown < 10000 && !WitherCloakTimer.active) {
                draw(gui, font, "§4§lWither Cloak\n§4cooldown " + fmt(10000 - (now - WitherCloakTimer.cooldown)), x, y, s);
            } else if (now - WitherCloakTimer.duration < 10000 && WitherCloakTimer.active) {
                draw(gui, font, "§a§lWither Cloak\n§aactivated " + fmt(10000 - (now - WitherCloakTimer.duration)), x, y, s);
            } else if (now - WitherCloakTimer.cooldown < 12000 && !WitherCloakTimer.active && WitherCloakTimer.cooldown > 0) {
                draw(gui, font, "§d§lWither Cloak\n§4§l   Ready!", x, y, s);
            }
        }

        // Soulward
        if (HudManager.shouldShow("SoulwardTimer")) {
            int x = HudManager.x("SoulwardTimer"), y = HudManager.y("SoulwardTimer");
            float s = HudManager.scale("SoulwardTimer");
            if (SoulwardTimer.duration > 0 && now - SoulwardTimer.duration < 5000) {
                draw(gui, font, "§1§lSoulward§7:§r §a" + fmt(5000 - (now - SoulwardTimer.duration)), x, y, s);
            } else if (SoulwardTimer.cooldown > 0 && now - SoulwardTimer.cooldown < 20000) {
                draw(gui, font, "§1§lSoulward§7:§r §4" + fmt(20000 - (now - SoulwardTimer.cooldown)), x, y, s);
            }
        }

        // Aligned
        if (HudManager.shouldShow("AlignedTimer")) {
            int x = HudManager.x("AlignedTimer"), y = HudManager.y("AlignedTimer");
            float s = HudManager.scale("AlignedTimer");
            if (now - AlignedTimer.time < 6000) {
                draw(gui, font, "§a§laligned §r§6" + fmt(6000 - (now - AlignedTimer.time)) + " §a|||\n" + AlignedTimer.by, x, y, s);
            }
        }

        // Gravity Storm
        if (HudManager.shouldShow("GravityStormTimer")) {
            int x = HudManager.x("GravityStormTimer"), y = HudManager.y("GravityStormTimer");
            float s = HudManager.scale("GravityStormTimer");
            if (now - GravityStormTimer.time < 30000) {
                draw(gui, font, "§5§lGravity Storm §r§b" + fmt(30000 - (now - GravityStormTimer.time)), x, y, s);
            }
        }
    }

    private static void draw(GuiGraphicsExtractor gui, net.minecraft.client.gui.Font font, String text, int x, int y, float s) {
        HudManager.drawScaled(gui, font, text, x, y, s);
    }

    private static String fmt(long ms) { long s = ms / 1000, m = (ms % 1000) / 10; return String.format("%d.%02ds", s, m); }
}
