package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.HudManager;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.HypixelLocationTracker;

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
        long now = System.currentTimeMillis();

        if (HudManager.shouldShow("SlayerBoss")) {
            BossDetector.updateHP();
            if (BossDetector.currentBoss != null && !BossDetector.currentBoss.isDeadOrDying() && BossDetector.bossMaxHP > 0) {
                int pct = (int)(BossDetector.bossHP / BossDetector.bossMaxHP * 100);
                gui.drawString(font, String.format("§4§l%s §c%d%% §7(%.0f/%.0f)",
                        BossDetector.bossType.isEmpty() ? "Boss" : BossDetector.bossType, pct, BossDetector.bossHP, BossDetector.bossMaxHP),
                        HudManager.x("SlayerBoss"), HudManager.y("SlayerBoss"), 0xFFFFFFFF, true);
            }
        }

        if (HudManager.shouldShow("PigmanSword") && PigmanSwordTimer.time > 0) {
            long rem = 5000 - (now - PigmanSwordTimer.time);
            if (rem > 0) gui.drawString(font, "§6Pigman: " + fmt(rem), HudManager.x("PigmanSword"), HudManager.y("PigmanSword"), 0xFFFFFFFF, true);
            else PigmanSwordTimer.time = 0;
        }

        RagnarockAxeTimer.update();
        if (HudManager.shouldShow("RagnarockAxe")) {
            int x = HudManager.x("RagnarockAxe"), y = HudManager.y("RagnarockAxe");
            if (RagnarockAxeTimer.castTime > now) gui.drawString(font, "§5Ragnarock: §b" + fmt(RagnarockAxeTimer.castTime - now), x, y, 0xFFFFFFFF, true);
            else if (RagnarockAxeTimer.duration > now) gui.drawString(font, "§5Ragnarock: §a" + fmt(RagnarockAxeTimer.duration - now), x, y, 0xFFFFFFFF, true);
            else if (RagnarockAxeTimer.cooldown > now && RagnarockAxeTimer.castTime == 0 && RagnarockAxeTimer.duration == 0 && !RagnarockAxeTimer.cancelled)
                gui.drawString(font, "§5Ragnarock: §c" + fmt(RagnarockAxeTimer.cooldown - now), x, y, 0xFFFFFFFF, true);
        }

        if (HudManager.shouldShow("EndStoneSword") && EndStoneSwordTimer.active) {
            long rem = 5000 - (now - EndStoneSwordTimer.time);
            if (rem > 0) gui.drawString(font, "§eEnd Stone Sword: §a" + (int)(rem/50f) + "% §7DR", HudManager.x("EndStoneSword"), HudManager.y("EndStoneSword"), 0xFFFFFFFF, true);
            else EndStoneSwordTimer.active = false;
        }

        if (HudManager.shouldShow("ReaperArmor") && ReaperArmorTimer.time > 0) {
            long rem = 15000 - (now - ReaperArmorTimer.time);
            if (rem > 0) gui.drawString(font, "§8Reaper Armor: §a" + fmt(rem), HudManager.x("ReaperArmor"), HudManager.y("ReaperArmor"), 0xFFFFFFFF, true);
            else ReaperArmorTimer.time = 0;
        }
    }

    private static String fmt(long ms) { long s = ms / 1000, m = (ms % 1000) / 10; return String.format("%d.%02ds", s, m); }
}
