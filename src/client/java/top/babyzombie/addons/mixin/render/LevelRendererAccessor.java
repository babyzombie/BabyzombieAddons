package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor("viewArea")
    ViewArea getViewArea();

    @Accessor("entityRenderDispatcher")
    EntityRenderDispatcher getEntityRenderDispatcher();

    @Accessor("chunkLayerSampler")
    GpuSampler getChunkLayerSampler();

    @Accessor("chunkLayerSampler")
    void setChunkLayerSampler(GpuSampler chunkLayerSampler);

    /// 第二相机捕获期间临时替换 outline 目标为子相机尺寸:
    /// 第二遍渲染的发光实体(原版/Skyblocker 的 data key 标记不经过
    /// shouldShowEntityOutlines)画进共享 entityOutlineTarget 后,主画面
    /// doEntityOutline 会把它全屏 blit 到主画面(发光放大平移污染/闪烁);
    /// 替换后子相机发光画进子相机 outline target,主画面保持自己的 outline。
    /// 注意:LevelRenderer.entityOutlineTarget() 是帧内句柄解析(renderLevel 后返回 null),
    /// 保存/恢复必须用本 accessor 读写实际字段。
    @Accessor("entityOutlineTarget")
    RenderTarget getEntityOutlineTarget();

    @Mutable
    @Accessor("entityOutlineTarget")
    void setEntityOutlineTarget(RenderTarget renderTarget);
}
