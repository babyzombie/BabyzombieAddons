package top.babyzombie.addons.util.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class CurrentEntityTracker {
    private CurrentEntityTracker() {}
    public static final ThreadLocal<EntityRenderState> STATE = new ThreadLocal<>();
}
