package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
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

    /// 第二相机捕获期间临时替换 outline 目标为子相机尺寸:
    /// 第二遍渲染的发光实体画进共享 entityOutlineTarget 后,主画面 doEntityOutline
    /// 会把它全屏 blit 到主画面(发光放大平移污染/闪烁);
    /// 替换后子相机发光画进子相机 outline target,主画面保持自己的 outline。
    @Accessor("entityOutlineTarget")
    RenderTarget getEntityOutlineTarget();

    @Accessor("entityOutlineTarget")
    void setEntityOutlineTarget(RenderTarget renderTarget);
}
