package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

public final class KuudraLocationTracker {
    private KuudraLocationTracker() {}

    public static String area = "p1&p2";
    public static LivingEntity kuudraEntity;
    public static float hp;
    public static boolean p4;
    public static boolean inKuudra;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!HypixelLocationTracker.getInstance().isInKuudra()) {
                if (inKuudra) reset();
                return;
            }
            if (!inKuudra) inKuudra = true;

            if (client.player != null && client.player.tickCount % 20 == 0)
                checkPlayerLocation(client);

            findKuudra(client);
        });
    }

    private static void checkPlayerLocation(Minecraft client) {
        var p = client.player;
        if (p == null) return;
        double x = p.getX(), y = p.getY(), z = p.getZ();

        if (x > -200 && y > 64 && z > -200 && x < -14 && y < 173 && z < -17) area = "p1&p2";
        else if (x > -186 && y > 14 && z > -197 && x < -130 && y < 63 && z < -134) area = "p3";
        else if (x > -133 && y > 0 && z > -137 && x < -74 && y < 63 && z < -76) area = "p4";
    }

    private static void findKuudra(Minecraft client) {
        if (client.player == null) return;

        var cubes = client.player.level().getEntitiesOfClass(MagmaCube.class,
                new AABB(client.player.blockPosition()).inflate(128),
                e -> e.getSize() == 30);
        if (!cubes.isEmpty()) {
            cubes.sort((a, b) -> Double.compare(b.getY(), a.getY()));
            kuudraEntity = cubes.getFirst();
        } else {
            kuudraEntity = null;
        }

        if (kuudraEntity != null && !kuudraEntity.isDeadOrDying()) {
            hp = kuudraEntity.getHealth();
        } else {
            var withers = client.player.level().getEntitiesOfClass(WitherBoss.class,
                    new AABB(client.player.blockPosition()).inflate(128),
                    e -> {
                        String name = ChatUtils.stripColor(e.getName().getString());
                        return name.contains("Kuudra");
                    });
            if (!withers.isEmpty())
                hp = withers.get(0).getHealth() / 300f * 100000f;
            else
                hp = 0;
        }
    }

    public static void reset() {
        if (kuudraEntity != null) {
            GlowController.setGlow(kuudraEntity, false, 0);
        }
        inKuudra = false;
        kuudraEntity = null;
        hp = 0;
        p4 = false;
        area = "p1&p2";
    }
}
