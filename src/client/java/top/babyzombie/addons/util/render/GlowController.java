package top.babyzombie.addons.util.render;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GlowController {
    private GlowController() {}

    private static final Map<UUID, Integer> GLOW_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> DEPTH_TEST = new ConcurrentHashMap<>();

    static {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((_, _) -> clearAll());
    }

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
        Boolean depthTest = DEPTH_TEST.get(entity.getUUID());
        setGlow(entity, glow, color, depthTest != null && depthTest);
    }

    /** Toggle glow on/off with a specific color and optional depth test. */
    public static void setGlow(Entity entity, boolean glow, int color, boolean depthTest) {
        UUID uuid = entity.getUUID();
        if (glow) {
            GLOW_DATA.put(uuid, color);
            if (depthTest) {
                DEPTH_TEST.put(uuid, true);
            } else {
                DEPTH_TEST.remove(uuid);
            }
        } else {
            GLOW_DATA.remove(uuid);
            DEPTH_TEST.remove(uuid);
        }
    }

    public static boolean shouldGlow(Entity entity) {
        return GLOW_DATA.containsKey(entity.getUUID());
    }

    public static int getGlowColor(Entity entity) {
        Integer color = GLOW_DATA.get(entity.getUUID());
        return color != null ? color : 0xFFFFFF;
    }

    /** 检查特定实体是否启用了深度测试发光。 */
    public static boolean isDepthTestEnabled(Entity entity) {
        return DEPTH_TEST.getOrDefault(entity.getUUID(), false);
    }

    /** 检查当前是否有任何实体需要深度测试发光。 */
    public static boolean isAnyDepthTestRequested() {
        return !DEPTH_TEST.isEmpty();
    }

    /** 清除所有发光数据（世界卸载时自动调用）。 */
    public static void clearAll() {
        GLOW_DATA.clear();
        DEPTH_TEST.clear();
    }
}
