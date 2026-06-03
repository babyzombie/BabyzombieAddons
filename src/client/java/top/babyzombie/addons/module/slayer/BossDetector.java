package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ServerTick;

/**
 * Detects slayer boss spawns and tracks HP.
 */
public final class BossDetector {
    static LivingEntity currentBoss;
    static String bossType = "";
    static float bossHP;
    static float bossMaxHP;
    static long spawnTime;

    private BossDetector() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());

            if (text.contains("SLAYER BOSS SPAWNED") || text.contains("A powerful")) {
                spawnTime = ServerTick.getTime();
                findBoss();
            }
            if (text.contains("SLAYER BOSS SLAIN")) {
                currentBoss = null;
                bossType = "";
                bossHP = 0;
                bossMaxHP = 0;
            }
        });
    }

    private static void findBoss() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var bosses = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(32),
                e -> e != player && !e.isDeadOrDying() && e.getMaxHealth() > 1000);
        if (!bosses.isEmpty()) {
            currentBoss = bosses.get(0);
            bossMaxHP = currentBoss.getMaxHealth();
            bossHP = currentBoss.getHealth();
        }
    }

    static void updateHP() {
        if (currentBoss != null && !currentBoss.isDeadOrDying()) {
            bossHP = currentBoss.getHealth();
        }
    }
}
