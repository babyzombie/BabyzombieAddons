package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FeatureRenderDispatcher.class)
public class FeatureRenderDispatcherMixin {

    @Shadow
    @Final
    private FeatureRenderDispatcher.PreparedFrame preparedFrame;

    /// 防御:PreparedFrame 是共享单例,begin 后 prepareFrameWithContext 抛异常时
    /// (try-with-resources 资源未建立,close 不被调用)会泄漏,下一帧任何调用方
    /// (LevelRenderer 主画面 renderLevel / GuiItemAtlas 物品图标)再 begin 都会抛
    /// "PreparedFrame already in use" 崩端。prepareFrame 是所有调用方必经入口,
    /// 在 begin 前复位残留状态:正常流程 frame 尚未 begin,复位无副作用;
    /// 泄漏场景丢弃异常帧的绘制数据,不再崩端。
    @Inject(method = "prepareFrame", at = @At("HEAD"))
    private void resetStaleFrame(CallbackInfoReturnable<FeatureRenderDispatcher.PreparedFrame> cir) {
        ((PreparedFrameAccessor) (Object) preparedFrame).setContext(null);
        ((PreparedFrameAccessor) (Object) preparedFrame).setSubmitNodeStorage(null);
    }
}
