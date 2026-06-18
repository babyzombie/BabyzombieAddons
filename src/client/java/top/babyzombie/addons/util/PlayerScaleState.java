package top.babyzombie.addons.util;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class PlayerScaleState {
    private PlayerScaleState() {}

    public static final Set<EntityRenderState> LOCAL_PLAYER_STATES =
            Collections.newSetFromMap(new IdentityHashMap<>());
}
