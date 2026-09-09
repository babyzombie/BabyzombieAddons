package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.state.gui.GlyphRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.ChatItemIconBridge;
import top.babyzombie.addons.util.render.ChatItemIconRenderable;
import top.babyzombie.addons.util.render.ChatItemIconSubmitter;

/**
 * 节点级提交触发器：{@code GuiRenderer.prepareText} 把每个字形包成 GlyphRenderState
 * 并调用 {@code GuiRenderState.addGlyphToCurrentLayer}——此刻 {@code current} 正是
 * 该聊天文本所在节点。若字形是物品图标字形，就把图标 blit 提交到同一节点
 * （与聊天文字同层，不盖暂停页模糊）。
 */
@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin {

    @Inject(method = "addGlyphToCurrentLayer", at = @At("HEAD"))
    private void babyzombie$onItemIconGlyph(GuiElementRenderState glyphState, CallbackInfo ci) {
        if (glyphState instanceof GlyphRenderState glyph
                && glyph.renderable() instanceof ChatItemIconRenderable icon) {
            Object renderer = ChatItemIconBridge.currentRenderer();
            if (renderer instanceof ChatItemIconSubmitter submitter) {
                submitter.submitChatIconBlit(icon);
            }
        }
    }
}