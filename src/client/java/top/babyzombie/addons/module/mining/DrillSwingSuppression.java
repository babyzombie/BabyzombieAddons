package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.HypixelLocationTracker;

public final class DrillSwingSuppression {
    private DrillSwingSuppression() {}

    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (!ModConfigManager.get().mining.drillSwingSuppression) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            var stack = player.getMainHandItem();
            if (stack.isEmpty()) return;
            var customData = stack.getComponents().get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (customData == null) return;
            var tag = customData.copyTag();
            if (tag == null) return;
            var ea = tag.getCompound("ExtraAttributes").orElse(null);
            if (ea == null) return;
            String id = ea.getString("id").orElse("");
            if (!id.isEmpty() && (id.contains("DRILL") || id.contains("GAUNTLET") || id.contains("PICKAXE"))) {
                player.swingTime = 0;
            }
        });
    }
}
