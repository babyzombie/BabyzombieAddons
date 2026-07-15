package top.babyzombie.addons.util;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class PlayerScaleState {
    private PlayerScaleState() {}

    public static final Set<EntityRenderState> LOCAL_PLAYER_STATES =
            Collections.newSetFromMap(new IdentityHashMap<>());

    /** 当前正在渲染的本地玩家透明度，提交完成后由 Mixin 清除 */
    public static final ThreadLocal<Float> CURRENT_ALPHA = ThreadLocal.withInitial(() -> 1.0f);
}
