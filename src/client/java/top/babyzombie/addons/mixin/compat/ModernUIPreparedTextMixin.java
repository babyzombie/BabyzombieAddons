package top.babyzombie.addons.mixin.compat;

import icyllis.modernui.mc.text.TextLayout;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.ChatItemIconBridge;
import top.babyzombie.addons.util.render.ChatItemIconRenderable;

import java.util.ArrayList;
import java.util.List;

/**
 * ModernUI 渲染端：把布局阶段收集到的物品图标注入 {@code ModernPreparedText}。
 *
 * <p>{@code ModernPreparedText} 是 ModernUI 的 {@code Font.PreparedText} 实现，
 * {@code submitRuns(renderState, pose, scissor)} 会把 {@code customRenderables} 里的每个
 * {@link TextRenderable} 以 {@code GlyphRenderState} 提交到 {@code renderState.addGlyphToCurrentLayer}——
 * 这正是我们 {@code GuiRenderStateMixin} 拦截的出口（{@code renderable instanceof ChatItemIconRenderable}
 * → {@code submitChatIconBlit}）。
 *
 * <p>本 mixin 在第二构造器（带 {@code TextLayout}）TAIL：
 * <ul>
 *   <li>用 {@code layout.getTextBuf()} 作 key，从 {@link ChatItemIconBridge} 查该文本的标记
 *       （布局缓存命中时也能取到，key 与布局阶段写入的一致）；</li>
 *   <li>用 {@link #positions}（glyph 序 x/y 数组）按下标定位 x 偏移（LTR 下 glyph 与字符 1:1），
 *       y 取行顶；</li>
 *   <li>构造 {@link ChatItemIconRenderable} 追加进 {@link #customRenderables}，
 *       由 {@code submitRuns} 统一提交。</li>
 * </ul>
 * 文本字形本身完全走 ModernUI 原生渲染，图标与文字同节点同层。
 */
@Pseudo
@Mixin(targets = "icyllis.modernui.mc.text.ModernPreparedText", remap = false)
public abstract class ModernUIPreparedTextMixin {

    @Shadow
    public float x;

    @Shadow
    private float top;

    @Shadow
    private float xAdj;

    @Shadow
    private float yAdj;

    @Shadow
    private float[] positions;

    @Shadow
    private ArrayList<TextRenderable> customRenderables;

    /**
     * 第二构造器 TAIL：追加图标字形。
     * 描述符 (FFIZIIFFF[BakedGlyph;TextLayout;)V
     */
    @Inject(method = "<init>(FFIZIIFFF" +
            "[Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;" +
            "Licyllis/modernui/mc/text/TextLayout;)V",
            at = @At("TAIL"), require = 0)
    private void babyzombie$injectIcons(float x0, float top0, int color, boolean dropShadow,
                                        int preferredMode, int bgColor, float xAdj0, float yAdj0,
                                        float density, BakedGlyph[] glyphs, TextLayout layout,
                                        CallbackInfo ci) {
        try {
            char[] textBuf = layout == null ? null : layout.getTextBuf();
            if (textBuf == null) return;
            List<ChatItemIconBridge.ModernMarker> markers =
                    ChatItemIconBridge.modernMarkersByText(new String(textBuf));
            if (markers == null || markers.isEmpty()) return;

            for (ChatItemIconBridge.ModernMarker marker : markers) {
                ItemStack stack = marker.stack();
                if (stack == null) continue;
                int idx = marker.index();
                // positions 是 glyph 序 x1,y1,x2,y2…；LTR 下下标即 glyph 下标
                if (positions == null || idx < 0 || idx * 2 + 1 >= positions.length) continue;
                float posX = x + positions[idx << 1] + xAdj;
                float posY = top + yAdj;

                customRenderables.add(new ChatItemIconRenderable(
                        posX, posY, stack,
                        ChatItemIconBridge.currentPose(),
                        ChatItemIconBridge.currentScissor(),
                        net.minecraft.network.chat.Style.EMPTY));
            }
        } catch (Throwable t) {
            // 渲染兜底：任何异常都不应影响 ModernUI 文本渲染
        }
    }
}