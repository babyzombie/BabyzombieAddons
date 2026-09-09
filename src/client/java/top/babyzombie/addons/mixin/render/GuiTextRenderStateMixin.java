package top.babyzombie.addons.mixin.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.util.render.ChatItemIconBridge;

/**
 * 捕获当前文本渲染状态的 pose/scissor。
 *
 * <p>{@code GuiTextRenderState.ensurePrepared()} 是字体逐字符处理（PreparedTextBuilder.accept）
 * 的父调用，必然先于它执行；在这里把该文本的 pose/scissor 写入 {@link ChatItemIconBridge}，
 * 供聊天物品图标字形构造时使用（保证 blit 与字形落在同一坐标空间）。
 */
@Mixin(GuiTextRenderState.class)
public abstract class GuiTextRenderStateMixin {

    @Inject(method = "ensurePrepared", at = @At("HEAD"))
    private void babyzombie$captureTextContext(CallbackInfoReturnable<Font.PreparedText> cir) {
        GuiTextRenderState self = (GuiTextRenderState) (Object) this;
        ChatItemIconBridge.setTextContext(self.pose, self.scissor);
    }
}