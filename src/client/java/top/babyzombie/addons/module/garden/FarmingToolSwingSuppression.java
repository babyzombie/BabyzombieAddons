package top.babyzombie.addons.module.garden;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

public final class FarmingToolSwingSuppression {
    private FarmingToolSwingSuppression() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().garden.farmingToolSwingSuppression) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            String location = HypixelLocationTracker.getInstance().getMap();
            if (location == null || !location.equals("Garden")) return;
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            if (!ItemUtils.isFarmingTool(player.getMainHandItem())) return;

            player.swingTime = 0;
            player.swinging = false;
            player.attackAnim = 0f;
            player.oAttackAnim = 0f;
        });
    }
}
