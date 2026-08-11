package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/// 26.2:globalSettingsUniform 无 getter,用 @Accessor 访问。
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Accessor("globalSettingsUniform")
    GlobalSettingsUniform globalSettingsUniform();
}
