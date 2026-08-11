package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.DynamicUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSystem.class)
public interface RenderSystemAccessor {

    /// 全局 DynamicUniforms 实例(第二相机捕获期间临时替换为独立实例用)
    @Accessor("dynamicUniforms")
    static DynamicUniforms getDynamicUniforms() {
        throw new AssertionError();
    }

    @Accessor("dynamicUniforms")
    static void setDynamicUniforms(DynamicUniforms dynamicUniforms) {
        throw new AssertionError();
    }
}
