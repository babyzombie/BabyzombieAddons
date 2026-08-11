package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/// 26.2:GlobalSettingsUniform.buffer 无 getter,
/// 第二相机捕获结束后需把 RenderSystem 全局 globals 状态指回主画面 buffer。
@Mixin(GlobalSettingsUniform.class)
public interface GlobalSettingsUniformAccessor {

    @Accessor("buffer")
    GpuBuffer buffer();
}
