package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.culling.Frustum;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Frustum.class)
public interface FrustumAccessor {

    /// 视锥相交检测器(offsetToFullyIncludeCameraCube 重写用)
    @Accessor("intersection")
    FrustumIntersection getIntersection();
}
