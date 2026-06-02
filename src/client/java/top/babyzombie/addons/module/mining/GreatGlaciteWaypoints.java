package top.babyzombie.addons.module.mining;

import java.util.ArrayList;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.WorldTextRenderer;
import top.babyzombie.addons.util.WorldTextRenderer.TextEntry;

public final class GreatGlaciteWaypoints {
    // Great Glacite block positions - not in config, always GPS-like feature
    private static final double[][] POSITIONS = {
            {52, 154, 274}, {18, 164, 303}, {99, 135, 318}, {110, 139, 337}, {-32, 130, 332}
    };

    private GreatGlaciteWaypoints() {}

    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (!ModConfigManager.get().mining.greatGlaciteWaypoints) return;
            if (!isInGlaciteArea()) return;

            var entries = new ArrayList<TextEntry>();
            for (var p : POSITIONS) {
                entries.add(new TextEntry("§bGreat Glacite", p[0] + 0.5, p[1], p[2] + 0.5, 0x00FFFF));
            }
            WorldTextRenderer.render(ctx.matrices(), entries);
        });
    }

    private static boolean isInGlaciteArea() {
        var t = HypixelLocationTracker.getInstance();
        if (!t.isInSkyblock() || !"Dwarven Mines".equals(t.getLocation())) return false;
        String loc = t.getLocation();
        return "Dwarven Base Camp".equals(loc) || "Glacite Tunnels".equals(loc) || "Great Glacite Lake".equals(loc);
    }
}
