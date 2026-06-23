package top.babyzombie.addons.mixin;

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
 */
@Pseudo
@Mixin(targets = "icyllis.modernui.mc.text.TextRenderType", remap = false)
public class ModernUITextRenderTypeMixin {

    @Redirect(method = "<clinit>",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;buildSnippet()Lcom/mojang/blaze3d/pipeline/RenderPipeline$Snippet;"),
            require = 0)
    private static RenderPipeline.Snippet addChromaLayout(RenderPipeline.Builder builder) {
        return builder.withUniform("Chroma", UniformType.UNIFORM_BUFFER).buildSnippet();
    }
}
