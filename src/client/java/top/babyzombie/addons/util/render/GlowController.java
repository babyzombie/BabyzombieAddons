package top.babyzombie.addons.util.render;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GlowController {
    private GlowController() {}

    /** EntityRenderState 上标记该实体是否需要深度测试发光 */
    public static final RenderStateDataKey<Boolean> NEEDS_DEPTH_TEST =
        RenderStateDataKey.create(() -> "babyzombie_needs_depth_test");
    /** 深度测试实体的 outline 颜色（不写 outlineColor 以免触发原版 outline） */
    public static final RenderStateDataKey<Integer> DEPTH_GLOW_COLOR =
        RenderStateDataKey.create(() -> "babyzombie_depth_glow_color");

    private static final Map<UUID, Integer> GLOW_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> DEPTH_TEST = new ConcurrentHashMap<>();

    static {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((_, _) -> clearAll());
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(_ -> {
            var world = net.minecraft.client.Minecraft.getInstance().level;
            if (world != null && world.getGameTime() % 20 == 0) {
                pruneDead();
            }
        });
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
    public static boolean isDepthTestEnabled(UUID uuid) {
        return DEPTH_TEST.getOrDefault(uuid, false);
    }

    /** 检查特定实体是否启用了深度测试发光。 */
    public static boolean isDepthTestEnabled(Entity entity) {
        return isDepthTestEnabled(entity.getUUID());
    }

    /** 检查当前是否有任何实体需要深度测试发光。 */
    public static boolean isAnyDepthTestRequested() {
        return !DEPTH_TEST.isEmpty();
    }

    /**
     * 清理已死亡/移除实体的发光数据。
     * 使用 entitiesForRendering() 构建有效 UUID 集合，避免 world.getEntity(uuid)
     * 在客户端找不到非玩家实体的问题。
     */
    public static void pruneDead() {
        var world = net.minecraft.client.Minecraft.getInstance().level;
        if (world == null) return;

        var valid = new java.util.HashSet<UUID>();
        for (var entity : world.entitiesForRendering()) {
            valid.add(entity.getUUID());
        }

        var dead = new java.util.ArrayList<UUID>();
        for (var uuid : GLOW_DATA.keySet()) {
            if (!valid.contains(uuid)) {
                dead.add(uuid);
            }
        }
        for (var uuid : dead) {
            GLOW_DATA.remove(uuid);
            DEPTH_TEST.remove(uuid);
        }
    }

    /** 清除所有发光数据（世界卸载时自动调用）。 */
    public static void clearAll() {
        GLOW_DATA.clear();
        DEPTH_TEST.clear();
    }
}
