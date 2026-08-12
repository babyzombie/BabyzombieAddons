package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * PreparedFrame 复位用:第二相机重跑 renderLevel 期间发光实体 outline 提交异常时,
 * begin 已执行但 close 未被调用(泄漏),主画面后续 GUI 渲染会抛 "PreparedFrame
 * already in use" 崩端;渲染前把残留字段复位,异常帧丢弃但不崩。
 */
@Mixin(FeatureRenderDispatcher.PreparedFrame.class)
public interface PreparedFrameAccessor {

    @Accessor("context")
    void setContext(FeatureFrameContext context);

    @Accessor("submitNodeStorage")
    void setSubmitNodeStorage(SubmitNodeStorage submitNodeStorage);
}
