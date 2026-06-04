package top.babyzombie.addons.util;

import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GlowController {
    private GlowController() {}

    private static final Map<UUID, Integer> GLOW_DATA = new ConcurrentHashMap<>();

    /** Just enable glow, keep existing or default white. */
    public static void setGlow(Entity entity) {
        setGlow(entity, true);
    }

    /** Toggle glow on/off. If no color was set, defaults to white. */
    public static void setGlow(Entity entity, boolean glow) {
        Integer existing = GLOW_DATA.get(entity.getUUID());
        setGlow(entity, glow, existing != null ? existing : 0xFFFFFF);
    }

    /** Toggle glow on/off with a specific color. */
    public static void setGlow(Entity entity, boolean glow, int color) {
        if (glow) {
            GLOW_DATA.put(entity.getUUID(), color);
        } else {
            GLOW_DATA.remove(entity.getUUID());
        }
    }

    public static boolean shouldGlow(Entity entity) {
        return GLOW_DATA.containsKey(entity.getUUID());
    }

    public static int getGlowColor(Entity entity) {
        Integer color = GLOW_DATA.get(entity.getUUID());
        return color != null ? color : 0xFFFFFF;
    }
}
