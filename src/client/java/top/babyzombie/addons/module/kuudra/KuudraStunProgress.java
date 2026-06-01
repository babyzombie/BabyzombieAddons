package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.HudManager;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

public final class KuudraStunProgress {
    private KuudraStunProgress() {}

    static final String[] stun = {"", "", "", ""};

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().kuudra.stunTimer) return;
            if (!"p3".equals(KuudraLocationTracker.area)) return;
            if (client.player == null || client.player.tickCount % 10 != 0) return;

            stun[0] = stun[1] = stun[2] = stun[3] = "";
            findStandText(client, -154, 21, -154, 0);
            findStandText(client, -151, 21, -171, 1);
            findStandText(client, -167, 21, -166, 2);

            var stands = client.player.level().getEntitiesOfClass(ArmorStand.class,
                    new AABB(client.player.blockPosition()).inflate(32),
                    e -> e.getName().getString().contains("Lava rises") && e.distanceToSqr(-157, 21, -163) < 4);
            if (!stands.isEmpty()) stun[3] = ChatUtils.stripColor(stands.get(0).getName().getString());
        });

        HudRenderCallback.EVENT.register((gui, delta) -> {
            if (!ModConfigManager.get().kuudra.stunTimer) return;
            if (stun[0].isEmpty() && stun[1].isEmpty() && stun[2].isEmpty()) return;

            var font = Minecraft.getInstance().font;
            int x = HudManager.x("InKuudraStun"), y = HudManager.y("InKuudraStun");
            String text = "      " + stun[0] + "\n  " + stun[1] + "  " + stun[2] + "\n" + stun[3];
            gui.drawString(font, text, x, y, 0xFFFFFFFF, true);
        });
    }

    private static void findStandText(Minecraft client, double x, double y, double z, int idx) {
        var stands = client.player.level().getEntitiesOfClass(ArmorStand.class,
                new AABB(client.player.blockPosition()).inflate(32),
                e -> e.getName().getString().contains("Pod Condition") && e.distanceToSqr(x, y, z) < 4);
        if (!stands.isEmpty())
            stun[idx] = ChatUtils.stripColor(stands.get(0).getName().getString())
                    .replace("Pod Condition: ", "");
    }
}
