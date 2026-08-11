package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelExtractor.class)
public interface LevelExtractorInvoker {

    @Invoker("extractEntity")
    EntityRenderState invokeExtractEntity(Entity entity, float partialTickTime);

    /// 按指定视锥重算可见区块列表(visibleSections):
    /// 第二相机捕获期间,extract 里的旋转变化检测可能因角度接近而跳过更新,
    /// 这里在 extract 前强制刷新,让区块挑选/方块实体提取按第二相机视角进行。
    @Invoker("applyFrustum")
    void invokeApplyFrustum(Frustum frustum);
}
