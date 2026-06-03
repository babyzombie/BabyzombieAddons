package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.ModConfigManager;

import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

public final class SlayerHUD {
    private SlayerHUD() {}

    public static void init() {
        HudRenderCallback.EVENT.register((gui, delta) -> {
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            render(gui);
        });
    }

    private static void render(net.minecraft.client.gui.GuiGraphics gui) {
        var font = Minecraft.getInstance().font;
        var config = ModConfigManager.get().slayer;
        long now = ServerTick.getTime();

        if (HudManager.shouldShow("SlayerBoss")) {
            BossDetector.updateHP();
            if (BossDetector.currentBoss != null && !BossDetector.currentBoss.isDeadOrDying() && BossDetector.bossMaxHP > 0) {
                int pct = (int)(BossDetector.bossHP / BossDetector.bossMaxHP * 100);
                String text = String.format("§4§l%s §c%d%% §7(%.0f/%.0f)",
                        BossDetector.bossType.isEmpty() ? "Boss" : BossDetector.bossType, pct, BossDetector.bossHP, BossDetector.bossMaxHP);
                int x = HudManager.x("SlayerBoss"), y = HudManager.y("SlayerBoss");
                float s = HudManager.scale("SlayerBoss");
                HudManager.drawScaled(gui, font, text, x, y, s);
            }
        }

        if (HudManager.shouldShow("PigmanSword") && PigmanSwordTimer.time > 0) {
            long rem = 5000 - (now - PigmanSwordTimer.time);
            if (rem > 0) drawHud(gui, font, "PigmanSword", "§6Pigman: " + fmt(rem));
            else PigmanSwordTimer.time = 0;
        }

        RagnarockAxeTimer.update();
        if (HudManager.shouldShow("RagnarockAxe")) {
            if (RagnarockAxeTimer.castTime > now)
                drawHud(gui, font, "RagnarockAxe", "§5Ragnarock: §b" + fmt(RagnarockAxeTimer.castTime - now));
            else if (RagnarockAxeTimer.duration > now)
                drawHud(gui, font, "RagnarockAxe", "§5Ragnarock: §a" + fmt(RagnarockAxeTimer.duration - now));
            else if (RagnarockAxeTimer.cooldown > now && RagnarockAxeTimer.castTime == 0 && RagnarockAxeTimer.duration == 0 && !RagnarockAxeTimer.cancelled)
                drawHud(gui, font, "RagnarockAxe", "§5Ragnarock: §c" + fmt(RagnarockAxeTimer.cooldown - now));
        }

        if (HudManager.shouldShow("EndStoneSword") && EndStoneSwordTimer.active) {
            long rem = 5000 - (now - EndStoneSwordTimer.time);
            if (rem > 0) drawHud(gui, font, "EndStoneSword", "§eEnd Stone Sword: §a" + (int)(rem/50f) + "% §7DR");
            else EndStoneSwordTimer.active = false;
        }

        if (HudManager.shouldShow("ReaperArmor") && ReaperArmorTimer.time > 0) {
            long rem = 15000 - (now - ReaperArmorTimer.time);
            if (rem > 0) drawHud(gui, font, "ReaperArmor", "§8Reaper Armor: §a" + fmt(rem));
            else ReaperArmorTimer.time = 0;
        }
    }

    private static void drawHud(net.minecraft.client.gui.GuiGraphics gui, net.minecraft.client.gui.Font font, String key, String text) {
        int x = HudManager.x(key), y = HudManager.y(key);
        float s = HudManager.scale(key);
        HudManager.drawScaled(gui, font, text, x, y, s);
    }

    private static String fmt(long ms) { long s = ms / 1000, m = (ms % 1000) / 10; return String.format("%d.%02ds", s, m); }
}
