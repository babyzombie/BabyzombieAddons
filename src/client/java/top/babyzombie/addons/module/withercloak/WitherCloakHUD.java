package top.babyzombie.addons.module.withercloak;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.HudManager;
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
        long now = WitherCloakTimer.now();

        if (ModConfigManager.get().witherCloak.witherCloakTimer) {
            if (now - WitherCloakTimer.cooldown < 10000 && !WitherCloakTimer.active) {
                gui.drawString(font, "§4§lWither Cloak\n§4cooldown " + fmt(10000 - (now - WitherCloakTimer.cooldown)), x, y, 0xFFFFFFFF, true);
                y += 20;
            } else if (now - WitherCloakTimer.duration < 10000 && WitherCloakTimer.active) {
                gui.drawString(font, "§a§lWither Cloak\n§aactivated " + fmt(10000 - (now - WitherCloakTimer.duration)), x, y, 0xFFFFFFFF, true);
                y += 20;
            } else if (now - WitherCloakTimer.cooldown < 12000 && !WitherCloakTimer.active && WitherCloakTimer.cooldown > 0) {
                gui.drawString(font, "§d§lWither Cloak\n§4§l   Ready!", x, y, 0xFFFFFFFF, true);
                y += 20;
            }
        }
        if (SoulwardTimer.duration > 0 && now - SoulwardTimer.duration < 5000) {
            gui.drawString(font, "§1§lSoulward§7:§r §a" + fmt(5000 - (now - SoulwardTimer.duration)), x, y, 0xFFFFFFFF, true);
            y += 12;
        } else if (SoulwardTimer.cooldown > 0 && now - SoulwardTimer.cooldown < 20000) {
            gui.drawString(font, "§1§lSoulward§7:§r §4" + fmt(20000 - (now - SoulwardTimer.cooldown)), x, y, 0xFFFFFFFF, true);
            y += 12;
        }
        if (now - AlignedTimer.time < 6000)
            gui.drawString(font, "§a§laligned §r§6" + fmt(6000 - (now - AlignedTimer.time)) + " §a|||\n" + AlignedTimer.by, x, y, 0xFFFFFFFF, true);
        y += 24;
        if (now - GravityStormTimer.time < 30000)
            gui.drawString(font, "§5§lGravity Storm §r§b" + fmt(30000 - (now - GravityStormTimer.time)), x, y, 0xFFFFFFFF, true);
    }

    private static String fmt(long ms) { long s = ms / 1000, m = (ms % 1000) / 10; return String.format("%d.%02ds", s, m); }
}
