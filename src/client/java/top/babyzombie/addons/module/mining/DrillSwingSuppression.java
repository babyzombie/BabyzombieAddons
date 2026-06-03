package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.ItemUtils;

public final class DrillSwingSuppression {
    private DrillSwingSuppression() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().mining.drillSwingSuppression) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            var stack = player.getMainHandItem();
            String id = ItemUtils.getSkyblockId(stack);
            if (id == null || (!id.contains("DRILL") && !id.contains("GAUNTLET") && !id.contains("PICKAXE")))
                return;

            player.swingTime = 0;
            player.swinging = false;
            player.attackAnim = 0f;
            player.oAttackAnim = 0f;
        });
    }
}
