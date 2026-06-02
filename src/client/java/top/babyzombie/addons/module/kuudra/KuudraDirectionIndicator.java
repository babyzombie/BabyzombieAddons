package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;

public final class KuudraDirectionIndicator {
    private KuudraDirectionIndicator() {}

    private static final Identifier ICON =
            Identifier.fromNamespaceAndPath("babyzombieaddons", "textures/gui/magmacube.png");

    public static void init() {
        HudRenderCallback.EVENT.register((gui, delta) -> {
            if (!ModConfigManager.get().kuudra.directionIndicator) return;

            var e = KuudraLocationTracker.kuudraEntity;
            if (e == null || e.isDeadOrDying() || KuudraLocationTracker.hp < 2) return;

            String area = KuudraLocationTracker.area;
            double kuudraX = e.getX(), kuudraZ = e.getZ();
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            double yawRad;
            if ("p4".equals(area)) {
                double dx = kuudraX + 7.5 - player.getX();
                double dz = kuudraZ + 7.5 - player.getZ();
                double angleToKuudra = Math.toDegrees(Math.atan2(-dx, dz));
                yawRad = Math.toRadians(player.getYRot() - angleToKuudra);
            } else if (KuudraLocationTracker.p4) {
                if (kuudraZ > -92) yawRad = 0;
                else if (kuudraX < -114) yawRad = Math.toRadians(270);
                else if (kuudraZ < -118) yawRad = Math.toRadians(180);
                else if (kuudraX > -89) yawRad = Math.toRadians(90);
                else return;
            } else return;

            int cx = HudManager.x("KuudraDir"), cy = HudManager.y("KuudraDir");
            float dist = HudManager.scale("KuudraDir") * 40f;
            int ox = (int) (-dist * Math.sin(yawRad));
            int oy = (int) (-dist * Math.cos(yawRad));
            int sz = Math.max(1, (int) (12 * HudManager.scale("KuudraDir")));
            gui.blit(ICON, cx + ox - sz / 2, cy + oy - sz / 2, 0, 0, sz, sz, sz, sz);
        });
    }
}
