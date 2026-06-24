package top.babyzombie.addons.module.garden;

import top.babyzombie.addons.util.render.RenderPhaseRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.render.BeamRenderer;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.awt.Color;

public final class PestDisplay {
    private PestDisplay() {}

    public static void init() {
        RenderPhaseRegister.register(ctx -> {
            if (!ModConfigManager.get().garden.pestDisplay) return;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isIn("Garden")) return;
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            player.level().getEntities(player, new AABB(player.blockPosition()).inflate(48),
                    e -> e instanceof LivingEntity le && e != player && !(e instanceof net.minecraft.world.entity.player.Player)
                            && le.getMaxHealth() >= 600 && le.getMaxHealth() <= 1200)
                    .forEach(e -> BeamRenderer.drawBeam(ctx,
                        e.getX(), e.getY(), e.getZ(),
                        2048, 0.15f, new Color(0, 255, 0, 180).getRGB()));
        });
    }
}
