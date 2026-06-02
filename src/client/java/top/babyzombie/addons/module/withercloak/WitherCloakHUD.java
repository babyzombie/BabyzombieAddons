package top.babyzombie.addons.module.withercloak;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.ModConfigManager;

import top.babyzombie.addons.util.HypixelLocationTracker;

public final class WitherCloakHUD {
    private WitherCloakHUD() {}

    public static void init() {
        HudRenderCallback.EVENT.register((gui, delta) -> {
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            if (!HudManager.shouldShow("WitherCloak")) return;
            render(gui);
        });
    }

    private static void render(net.minecraft.client.gui.GuiGraphics gui) {
        var font = Minecraft.getInstance().font;
        int x = HudManager.x("WitherCloak");
        int y = HudManager.y("WitherCloak");
        float s = HudManager.scale("WitherCloak");
        long now = WitherCloakTimer.now();

        if (ModConfigManager.get().witherCloak.witherCloakTimer) {
            if (now - WitherCloakTimer.cooldown < 10000 && !WitherCloakTimer.active) {
                drawStr(gui, font, "§4§lWither Cloak\n§4cooldown " + fmt(10000 - (now - WitherCloakTimer.cooldown)), x, y, s);
                y += (int)(20 * s);
            } else if (now - WitherCloakTimer.duration < 10000 && WitherCloakTimer.active) {
                drawStr(gui, font, "§a§lWither Cloak\n§aactivated " + fmt(10000 - (now - WitherCloakTimer.duration)), x, y, s);
                y += (int)(20 * s);
            } else if (now - WitherCloakTimer.cooldown < 12000 && !WitherCloakTimer.active && WitherCloakTimer.cooldown > 0) {
                drawStr(gui, font, "§d§lWither Cloak\n§4§l   Ready!", x, y, s);
                y += (int)(20 * s);
            }
        }
        if (SoulwardTimer.duration > 0 && now - SoulwardTimer.duration < 5000) {
            drawStr(gui, font, "§1§lSoulward§7:§r §a" + fmt(5000 - (now - SoulwardTimer.duration)), x, y, s);
            y += (int)(12 * s);
        } else if (SoulwardTimer.cooldown > 0 && now - SoulwardTimer.cooldown < 20000) {
            drawStr(gui, font, "§1§lSoulward§7:§r §4" + fmt(20000 - (now - SoulwardTimer.cooldown)), x, y, s);
            y += (int)(12 * s);
        }
        if (now - AlignedTimer.time < 6000)
            drawStr(gui, font, "§a§laligned §r§6" + fmt(6000 - (now - AlignedTimer.time)) + " §a|||\n" + AlignedTimer.by, x, y, s);
        y += (int)(24 * s);
        if (now - GravityStormTimer.time < 30000)
            drawStr(gui, font, "§5§lGravity Storm §r§b" + fmt(30000 - (now - GravityStormTimer.time)), x, y, s);
    }

    private static void drawStr(net.minecraft.client.gui.GuiGraphics gui, net.minecraft.client.gui.Font font, String text, int x, int y, float s) {
        HudManager.drawScaled(gui, font, text, x, y, s);
    }

    private static String fmt(long ms) { long s = ms / 1000, m = (ms % 1000) / 10; return String.format("%d.%02ds", s, m); }
}
