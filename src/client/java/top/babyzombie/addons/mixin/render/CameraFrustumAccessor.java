package top.babyzombie.addons.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/// 访问 Camera 的 frustum 状态:
/// - captureFrustum 标志:26.2 主画面流程会 captureFrustum() 缓存视锥,
///   第二相机捕获期间需临时关闭,否则 extract 里 getCapturedFrustum() != null 会跳过可见区块更新;
/// - cullFrustum:update 按当时朝向计算,setRotation 之后方向过期,需要手动注入第二相机视锥。
@Mixin(Camera.class)
public interface CameraFrustumAccessor {

    @Accessor("captureFrustum")
    boolean captureFrustum();

    @Accessor("captureFrustum")
    void setCaptureFrustum(boolean captureFrustum);

    @Accessor("cullFrustum")
    void setCullFrustum(Frustum cullFrustum);
}
