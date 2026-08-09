package top.babyzombie.addons.mixin.window;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露 MC 自带的"最后输入时间":键盘/鼠标输入到达时 MC 会更新该字段
 * (用于 AFK 帧率限制),读取它即可做挂机判定,无需自己监听输入。
 */
@Mixin(FramerateLimitTracker.class)
public interface FramerateLimitTrackerAccessor {

    @Accessor
    long getLatestInputTime();
}
