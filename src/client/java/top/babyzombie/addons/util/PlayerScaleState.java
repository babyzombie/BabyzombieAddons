package top.babyzombie.addons.util;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

public final class PlayerScaleState {
    private PlayerScaleState() {}

    /// EntityRenderState 上标记该渲染状态属于本地玩家。
    /// 必须用 RenderStateDataKey（Fabric 的 render state 附加数据）挂在状态本身上：
    /// 渲染状态每帧重建、随帧回收，不能再用 static 集合按引用记录，否则集合只增不减导致内存泄漏。
    public static final RenderStateDataKey<Boolean> IS_LOCAL_PLAYER_STATE =
            RenderStateDataKey.create(() -> "babyzombie_local_player_render_state");

    /** 当前正在渲染的本地玩家透明度，提交完成后由 Mixin 清除 */
    public static final ThreadLocal<Float> CURRENT_ALPHA = ThreadLocal.withInitial(() -> 1.0f);
}
