package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * Kuudra 方向提示 — P4 开始前用绝对方向，下去后用玩家朝向的相对方向。
 */
public final class KuudraDirectionHUD {
    private KuudraDirectionHUD() {}

    // Absolute direction by Kuudra position
    private record Dir(String name, String color) {}
    private static final Dir UNKNOWN = new Dir("?", "§7");

    private static Dir lastDir = UNKNOWN;
    private static boolean p4 = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().kuudra.phase4.directionHud) return;
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            if (!KuudraLocationTracker.p4 && !"p4".equals(KuudraLocationTracker.area)) {
                lastDir = UNKNOWN;
                p4 = false;
                return;
            }
            if (client.player == null) return;

            // Find Kuudra entity (reuse location tracker or scan ourselves)
            var e = KuudraLocationTracker.kuudraEntity;
            if (e == null || e.isDeadOrDying()) {
                // Fallback: scan withers
                var withers = client.player.level().getEntitiesOfClass(
                        net.minecraft.world.entity.boss.wither.WitherBoss.class,
                        new net.minecraft.world.phys.AABB(client.player.blockPosition()).inflate(128),
                        w -> top.babyzombie.addons.util.ChatUtils.stripColor(
                                w.getName().getString()).contains("Kuudra"));
                if (!withers.isEmpty()) e = withers.getFirst();
            }
            if (e == null) {
                lastDir = UNKNOWN; // 实体丢失时清空，不显示上一次的方向
                return;
            }

            lastDir = getAbsoluteDir(e);
            
            if (!p4 && lastDir != UNKNOWN && KuudraStunTimer.p4End - ServerTick.getTime() > 12000) {
                KuudraStunTimer.p4End = ServerTick.getTime() + 5500;
                p4 = true;
            }
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_direction"),
                (context, tickCounter) -> {
                    if (!ModConfigManager.get().kuudra.phase4.directionHud) return;
                    if (lastDir == UNKNOWN) return;
                    if (!KuudraLocationTracker.p4 && !"p4".equals(KuudraLocationTracker.area)) return;

                    var font = Minecraft.getInstance().font;
                    int x = HudManager.x("KuudraDir"), y = HudManager.y("KuudraDir");
                    float s = HudManager.scale("KuudraDir");

                    // Below ground (P4 下去后 Y 稳定 <20): use player-relative direction only
                    boolean belowGround = Minecraft.getInstance().player != null
                            && Minecraft.getInstance().player.getY() < 20;
                    String text = belowGround
                            ? getRelativeDir(lastDir)          // 相对方向（不再拼接绝对方向）
                            : lastDir.color + "§l" + lastDir.name; // 地面时只显示绝对方向
                    HudManager.drawScaled(context, font, text, x, y, s);
                });

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
            p4 = false; lastDir = UNKNOWN;
        });
    }

    private static Dir getAbsoluteDir(LivingEntity e) {
        double x = e.getX(), z = e.getZ();
        if (x < -128) return new Dir("RIGHT", "§a");
        if (z > -84)  return new Dir("FRONT", "§c");
        if (x > -72)  return new Dir("LEFT", "§e");
        if (z < -132) return new Dir("BACK", "§9");
        return UNKNOWN;
    }

    /** Convert absolute direction to player-relative based on current yaw. */
    private static String getRelativeDir(Dir abs) {
        var player = Minecraft.getInstance().player;
        if (player == null) return abs.color + abs.name;

        float yaw = player.getYRot() % 360;
        if (yaw < 0) yaw += 360;

        // Absolute direction → world-space angle
        double kuudraAngle = switch (abs.name) {
            case "FRONT" -> 0;    // north / -Z
            case "BACK"  -> 180;  // south / +Z
            case "LEFT"  -> 270;  // west / -X
            case "RIGHT" -> 90;   // east / +X
            default -> 0;
        };

        double diff = ((kuudraAngle - yaw) % 360 + 360) % 360;
        if (diff > 180) diff -= 360;

        // 左右交换：MC yaw 顺时针，diff 为正时目标在右侧
        if (diff > -45 && diff <= 45)   return "§a▲ FRONT";
        if (diff > 45 && diff <= 135)   return "§9► RIGHT";
        if (diff > 135 || diff <= -135) return "§c▼ BACK";
        return "§e◄ LEFT";
    }
}
