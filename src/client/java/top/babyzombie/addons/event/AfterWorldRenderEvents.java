package top.babyzombie.addons.event;

import net.minecraft.client.DeltaTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/// "世界渲染完成后"事件(BZA 自有):
/// 在 GameRenderer.render 的世界渲染(renderLevel)完全返回后触发,
/// 由 {@link top.babyzombie.addons.mixin.render.GameRendererCameraCaptureMixin} 派发。
/// 第二相机等需要在世界渲染后做额外渲染的模块统一注册此事件,无需各自写 mixin。
/// 为什么不用 Fabric 的 LevelRenderEvents.END_MAIN:它触发于主 pass 完成后、
/// 云/天气之前,此时重跑 renderLevel 会把 Iris 的 pipeline 状态清掉,
/// 主画面后续云/天气 pass 直接 NPE(实测崩溃);renderLevel 完全返回后
/// 再渲染则没有这个问题(主画面渲染已结束)。
public final class AfterWorldRenderEvents {

    private static final List<Consumer<DeltaTracker>> CALLBACKS = new ArrayList<>();

    private AfterWorldRenderEvents() {}

    public static void register(Consumer<DeltaTracker> callback) {
        CALLBACKS.add(callback);
    }

    /// 由 mixin 在 renderLevel 完全返回后调用
    public static void fire(DeltaTracker deltaTracker) {
        for (Consumer<DeltaTracker> callback : CALLBACKS) {
            callback.accept(deltaTracker);
        }
    }
}
