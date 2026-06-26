package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.render.Waypoints;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

final class JungleTempleThinWall {
    // 卫兵中心 → 宝藏房薄墙偏移
    private static final int DX = 32;
    private static final int DY = -38;
    private static final int DZ = 34;
    static boolean shown;

    private JungleTempleThinWall() {}

    static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (shown) return;
            if (!ModConfigManager.get().mining.jungleTempleThinWall) return;

            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock() || !tracker.isIn("Crystal Hollows")) return;

            String text = ChatUtils.stripColor(message.getString());
            if (!text.startsWith("[NPC] Kalhuiki Door Guardian:")) return;

            var mc = Minecraft.getInstance();
            var player = mc.player;
            if (player == null) return;

            var level = player.level();
            var guardians = level.getEntities(player, player.getBoundingBox().inflate(20),
                    e -> e.getName().getString().contains("Kalhuiki Door Guardian"));

            if (guardians.size() < 2) return;

            var g1 = guardians.get(0).blockPosition();
            var g2 = guardians.get(1).blockPosition();
            int midX = (g1.getX() + g2.getX()) / 2;
            int midY = (g1.getY() + g2.getY()) / 2;
            int midZ = (g1.getZ() + g2.getZ()) / 2;

            int wx = midX + DX;
            int wy = midY + DY;
            int wz = midZ + DZ;

            shown = true;

            // 添加 waypoint，切世界自动清除
            String wpName = ChatUtils.translate("babyzombieaddons.jungleTemple.thinWall");
            Waypoints.addWaypoint(wpName, wx, wy, wz, 0, false);

            player.playSound(
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(),
                    0.8f, 1.2f);
        });
    }
}
