package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/// 访问 Minecraft.mainRenderTarget(private final),用于第二相机捕获时临时替换渲染目标。
@Mixin(Minecraft.class)
public interface MainRenderTargetAccessor {

    @Mutable
    @Accessor("mainRenderTarget")
    void setMainRenderTarget(RenderTarget target);
}
