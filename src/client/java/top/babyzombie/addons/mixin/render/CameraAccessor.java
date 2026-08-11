package top.babyzombie.addons.mixin.render;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.class)
public interface CameraAccessor {

    @Accessor("depthFar")
    void setDepthFar(float depthFar);

    @Accessor("eyeHeight")
    void setEyeHeight(float eyeHeight);

    @Accessor("eyeHeightOld")
    void setEyeHeightOld(float eyeHeightOld);
}
