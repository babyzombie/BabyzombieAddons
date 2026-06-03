package top.babyzombie.addons.module.mining;

import java.util.ArrayList;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.WorldRenderUtils;
import top.babyzombie.addons.util.WorldTextRenderer;
import top.babyzombie.addons.util.WorldTextRenderer.TextEntry;

public final class GreatGlaciteWaypoints {
    private static final double[][] POSITIONS = {
            {52, 154, 274}, {18, 164, 303}, {99, 135, 318}, {110, 139, 337}, {-32, 130, 332}
    };
    private static final double SIZE = 2.0;

    private GreatGlaciteWaypoints() {}

    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (!ModConfigManager.get().mining.greatGlaciteWaypoints) return;
            if (!isInGlaciteArea()) return;

            var entries = new ArrayList<TextEntry>();
            for (var p : POSITIONS) {
                double x = p[0], y = p[1], z = p[2];
                WorldRenderUtils.drawBox(
                    x - SIZE / 2, y, z - SIZE / 2,
                    x + SIZE / 2, y + SIZE, z + SIZE / 2,
                    0, 1, 1, 0.6f);
                entries.add(new TextEntry("§bGreat Glacite", x + 0.5, y + SIZE + 0.3, z + 0.5, 0x00FFFF));
            }
            WorldTextRenderer.render(ctx.matrices(), entries);
        });
    }

    private static boolean isInGlaciteArea() {
        var t = HypixelLocationTracker.getInstance();
        if (!t.isInSkyblock() || !"Dwarven Mines".equals(t.getMap())) return false;
        String loc = t.getLocation();
        return "Dwarven Base Camp".equals(loc) || "Glacite Tunnels".equals(loc) || "Great Glacite Lake".equals(loc);
    }
}
