package top.babyzombie.addons.mixin.render;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraInvoker {

    /// 强制设置相机旋转(绕过实体 getViewYRot/getViewXRot 的转换)
    @Invoker("setRotation")
    void invokeSetRotation(float yRot, float xRot);

    /// 按当前相机实体重算相机位置/朝向(恢复玩家视角用)
    @Invoker("alignWithEntity")
    void invokeAlignWithEntity(float partialTicks);
}
