package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor("viewArea")
    ViewArea getViewArea();

    @Accessor("entityRenderDispatcher")
    EntityRenderDispatcher getEntityRenderDispatcher();

    /// 区块纹理采样器(白线缓解):第二相机捕获期间换成 NEAREST mag + 禁 mip 的采样器,
    /// 防 RGSS 按小视口算 mip 层级导致图集边界 texel 渗漏白线
    @Accessor("chunkLayerSampler")
    GpuSampler getChunkLayerSampler();

    @Accessor("chunkLayerSampler")
    void setChunkLayerSampler(GpuSampler chunkLayerSampler);

    /// 第二相机捕获期间临时替换 outline 目标为子相机尺寸:
    /// 第二遍渲染的发光实体画进共享 entityOutlineTarget 后,主画面 doEntityOutline
    /// 会把它全屏 blit 到主画面(发光放大平移污染/闪烁);
    /// 替换后子相机发光画进子相机 outline target,主画面保持自己的 outline。
    @Accessor("entityOutlineTarget")
    RenderTarget getEntityOutlineTarget();

    @Accessor("entityOutlineTarget")
    void setEntityOutlineTarget(RenderTarget renderTarget);
}
