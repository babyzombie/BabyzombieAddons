package top.babyzombie.addons.mixin.compat;

import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 给 Modern UI 的文字渲染管线注入 Chroma UBO 声明，
 * 让 Aaron Mod 的 {@code RenderSystemMixin} 每帧绑定的 Chroma 计时器数据
 * 能被 Modern UI 的 shader 读到。
 *
 * <p>26.2 中 {@code RenderPipeline.Builder#withUniform(String, UniformType)} 已移除，
 * UBO 改为通过 {@link BindGroupLayout} 声明后再挂到 builder 上。
 *
 * <p>注意：布局必须在本方法内即时构建，不能缓存成 mixin 静态字段 ——
 * Mixin 合并静态成员进目标类时，字段初始化可能晚于 <clinit> 中 handler 的执行点，
 * 导致读取到 null 的 layout，进而在 {@code Builder.build()} 的
 * {@code List.copyOf} 处抛 NPE（26.2 崩过一次）。
 */
@Pseudo
@Mixin(targets = "icyllis.modernui.mc.text.TextRenderType", remap = false)
public class ModernUITextRenderTypeMixin {

    @Redirect(method = "<clinit>",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;buildSnippet()Lcom/mojang/blaze3d/pipeline/RenderPipeline$Snippet;"),
            require = 0)
    private static RenderPipeline.Snippet addChromaLayout(RenderPipeline.Builder builder) {
        return builder.withBindGroupLayout(
                        BindGroupLayout.builder()
                                .withUniform("Chroma", UniformType.UNIFORM_BUFFER)
                                .build())
                .buildSnippet();
    }
}