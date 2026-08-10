package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.module.fishing.FishingCameraModule;

/// 第二相机捕获期间,实体/方块实体的"区块可见性"检查忽略遮挡可见度。
/// 遮挡可见度(visibility)按主相机视锥维护,第二相机视角下浮标所在区块会被误判不可见,
/// 导致鱼漂实体和名字被剔除。
@Mixin(LevelRenderer.class)
public class SectionVisibilityCaptureMixin {

    @Inject(method = "isSectionCompiledAndVisible", at = @At("HEAD"), cancellable = true)
    private void babyzombieaddons$captureVisible(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (!FishingCameraModule.capturing) return;
        var section = ((ViewAreaInvoker) ((LevelRendererAccessor) this).getViewArea()).invokeGetRenderSectionAt(blockPos);
        cir.setReturnValue(section != null && section.getSectionMesh() != CompiledSectionMesh.UNCOMPILED);
    }
}
