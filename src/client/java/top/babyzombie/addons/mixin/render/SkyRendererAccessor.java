package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/// 26.2:SkyRenderer 缓存构造时的 renderTarget(private final),
/// 第二相机捕获期间需临时指向子相机输出,否则第二遍渲染的天空画到主画面(黄块/蓝块)。
/// @Mutable 让 mixin 移除字段 final 标志,否则 setter 触发 JVM final 字段写保护崩溃。
@Mixin(SkyRenderer.class)
public interface SkyRendererAccessor {

    @Mutable
    @Accessor("renderTarget")
    void setRenderTarget(RenderTarget renderTarget);
}
