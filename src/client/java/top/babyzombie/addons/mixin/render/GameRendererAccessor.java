package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/// 26.2:globalSettingsUniform 无 getter,用 @Accessor 访问。
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Accessor("globalSettingsUniform")
    GlobalSettingsUniform globalSettingsUniform();

    /// 主画面投影矩阵 buffer(第二相机捕获结束后写回主投影用)
    @Accessor("levelProjectionMatrixBuffer")
    ProjectionMatrixBuffer levelProjectionMatrixBuffer();
}
