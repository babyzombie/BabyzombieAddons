package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.LevelRenderer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.render.GlowRenderer;

/**
 * 在 entity_outline target 清空后、submitEntities 之前，
 * 将主场景深度拷贝到 entity_outline 的深度纹理中。
 * 配合 {@link RenderPipelineMixin} 启用 OUTLINE 管线的深度测试，
 * 实现不透墙的实体发光。
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    private @Nullable RenderTarget entityOutlineTarget;

    @Inject(
            method = {"lambda$addMainPass$0"},
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;shouldShowEntityOutlines()Z")
            ),
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;ILcom/mojang/blaze3d/textures/GpuTexture;D)V",
                    ordinal = 0,
                    shift = Shift.AFTER
            )
    )
    private void onOutlineTargetCleared(CallbackInfo ci) {
        if (!GlowController.isAnyDepthTestRequested()) return;
        if (entityOutlineTarget == null) return;

        var mainDepth = GlowRenderer.getMainDepthTexture();
        var outlineDepth = entityOutlineTarget.getDepthTexture();
        if (mainDepth == null || outlineDepth == null) return;
        if (mainDepth.getWidth(0) != outlineDepth.getWidth(0)
                || mainDepth.getHeight(0) != outlineDepth.getHeight(0)) return;

        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                mainDepth, outlineDepth,
                0, 0, 0, 0, 0,
                mainDepth.getWidth(0), mainDepth.getHeight(0)
        );

        GlowRenderer.markDepthTestActive();
    }
}
