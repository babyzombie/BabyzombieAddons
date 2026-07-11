package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.OutlineBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.GlowRenderer;

/**
 * 在 outline 绘制完成后恢复深度状态。
 * 注意：深度覆盖的设置在 {@link LevelRendererMixin} 中提前完成（render pass 创建前），
 * 本 mixin 只负责收尾恢复。
 */
@Mixin(OutlineBufferSource.class)
public class OutlineBufferSourceMixin {

    @Inject(method = "endOutlineBatch", at = @At("TAIL"))
    private void afterEndOutlineBatch(CallbackInfo ci) {
        GlowRenderer.endDepthTestedOutline();
    }
}
