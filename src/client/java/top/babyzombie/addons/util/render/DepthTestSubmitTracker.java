package top.babyzombie.addons.util.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 在 submitEntities 到 feature rendering 之间传递深度测试标志。
 * submitNode → needsDepthTest 的映射，使用身份比较（==）。
 */
public final class DepthTestSubmitTracker {
    private DepthTestSubmitTracker() {}

    /** 当前实体是否需要在 submit node 创建时被标记为深度测试 */
    public static final ThreadLocal<EntityRenderState> CURRENT_ENTITY_STATE = new ThreadLocal<>();

    /** submitNode (identity) → needsDepthTest */
    public static final Map<Object, Boolean> FLAGS = new IdentityHashMap<>();

    public static void clear() { FLAGS.clear(); }

    public static void mark(Object submitNode) { FLAGS.put(submitNode, true); }

    public static boolean consume(Object submitNode) {
        return FLAGS.remove(submitNode) != null;
    }
}
