package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.HudManager;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.HypixelLocationTracker;

/**
 * Displays Kuudra's HP on the HUD.
 */

public final class KuudraHPDisplay {
    static LivingEntity kuudraEntity;
    static float hp;
    static float maxHP = 100000;
    static boolean inKuudra;

    private KuudraHPDisplay() {}

    public static void init() {
        if (!ModConfigManager.get().kuudra.hpDisplay) return;

        HudRenderCallback.EVENT.register((gui, delta) -> {
            if (!inKuudra || !HypixelLocationTracker.getInstance().isInSkyblock()) return;
            findKuudra();
            render(gui);
        });
    }

    private static void findKuudra() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (kuudraEntity != null && kuudraEntity.isDeadOrDying()) kuudraEntity = null;

        if (kuudraEntity == null) {
            var cubes = player.level().getEntitiesOfClass(LivingEntity.class,
                    new AABB(player.blockPosition()).inflate(64),
                    e -> e.getBoundingBox().getSize() > 10 && e.getMaxHealth() >= 100000);
            if (!cubes.isEmpty()) {
                kuudraEntity = cubes.get(0);
                maxHP = kuudraEntity.getMaxHealth();
            }
        }
        if (kuudraEntity != null) hp = kuudraEntity.getHealth();
    }

    private static void render(net.minecraft.client.gui.GuiGraphics gui) {
        if (kuudraEntity == null || kuudraEntity.isDeadOrDying()) return;
        int pct = maxHP > 0 ? (int)(hp / maxHP * 100) : 0;
        var font = Minecraft.getInstance().font;
        int x = HudManager.x("KuudraHP"), y = HudManager.y("KuudraHP");
        gui.drawString(font, String.format("§b§lKuudra §c%d%% §7(%.0f/%.0f)", pct, hp, maxHP),
                x, y, 0xFFFFFFFF, true);
    }
}
