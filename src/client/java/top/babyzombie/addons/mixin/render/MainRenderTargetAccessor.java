package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/// 26.2:mainRenderTarget 从 Minecraft 挪到了 GameRenderer(private final),用 @Accessor + @Mutable 替换。
@Mixin(GameRenderer.class)
public interface MainRenderTargetAccessor {

    @Mutable
    @Accessor("mainRenderTarget")
    void setMainRenderTarget(RenderTarget target);
}
