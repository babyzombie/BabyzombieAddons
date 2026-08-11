package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.util.render.SecondCameraRenderer;

/// 第二相机捕获期间,实体/方块实体的"区块可见性"检查直接放行。
/// 遮挡可见度(visibility)按主相机视锥维护,第二相机视角下浮标所在区块会被误判不可见,
/// 导致鱼漂实体和名字被剔除;Sodium 下区块网格编译状态走它的渲染器,LevelRenderer 的
/// "已编译"判定也不可靠,直接返回 true(第二相机视锥内实体都提取)。
@Mixin(LevelRenderer.class)
public class SectionVisibilityCaptureMixin {

    @Inject(method = "isSectionCompiledAndVisible", at = @At("HEAD"), cancellable = true)
    private void babyzombieaddons$captureVisible(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (SecondCameraRenderer.capturing) {
            cir.setReturnValue(true);
        }
    }
}
