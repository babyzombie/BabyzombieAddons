package top.babyzombie.addons.mixin.compat;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.util.render.ChatItemIconBridge;
import top.babyzombie.addons.util.render.ChatItemIconSupport;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * ModernUI 文本布局阶段的物品图标标记收集（直接混入 ModernUI 内部）。
 *
 * <p>ModernUI 用自研 {@code TextLayoutProcessor} 排版文本：{@code GuiTextRenderState.ensurePrepared}
 * 经 @Redirect 直接走 {@code TextLayoutEngine.lookupFormattedLayout}，聊天文本以
 * {@code FormattedTextWrapper} 形态进入 {@code createTextLayout}，vanilla 的
 * {@code Font$PreparedTextBuilder} 完全被绕过——我们的 vanilla 侧图标注入失效。
 *
 * <p>本 mixin 只<b>替换三个布局入口的字符输入</b>（原方法体完全照常执行）：
 * <ul>
 *   <li>遇到标记码点 {@link ChatItemIconSupport#MARKER} 且 style 可解析出物品时，
 *       替换为 <b>EM SPACE（U+2003）</b> 透传给布局器：文本正常占位排版；</li>
 *   <li>同时记录标记字符在剥离文本中的下标 + ItemStack，布局完成后按剥离文本写入
 *       {@link ChatItemIconBridge} 全局表，供 {@code ModernPreparedText} 构造时生成图标字形。</li>
 * </ul>
 * 布局引擎其余部分（bidi/整形/缓存/渲染）完全不改，文本渲染保持 ModernUI 原生效果。
 */
@Pseudo
@Mixin(targets = "icyllis.modernui.mc.text.TextLayoutProcessor", remap = false)
public abstract class ModernUITextProcessorMixin {

    /** ModernUI 布局器的逐字符收集器（私有字段，仅供 createTextLayout 分支引用）。 */
    @Shadow
    private FormattedCharSink mSequenceBuilder;

    private static Method GET_TEXT_BUF;

    // ---- createSequenceLayout(FormattedCharSequence, int, int) ----
    @Redirect(method = "createSequenceLayout",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/FormattedCharSequence;accept" +
                            "(Lnet/minecraft/util/FormattedCharSink;)Z"),
            require = 0)
    private boolean babyzombie$wrapSequenceAccept(FormattedCharSequence sequence,
                                                  FormattedCharSink sink) {
        ChatItemIconBridge.startModernMarkers();
        return sequence.accept(babyzombie$wrapped(sink));
    }

    @Inject(method = "createSequenceLayout", at = @At("RETURN"), require = 0)
    private void babyzombie$storeSequence(CallbackInfoReturnable<Object> cir) {
        finishCollect(cir.getReturnValue());
    }

    // ---- createVanillaLayout(String, Style, int, int) ----
    @Redirect(method = "createVanillaLayout",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/StringDecomposer;iterateFormatted" +
                            "(Ljava/lang/String;Lnet/minecraft/network/chat/Style;" +
                            "Lnet/minecraft/util/FormattedCharSink;)Z"),
            require = 0)
    private boolean babyzombie$wrapVanillaIterate(String text, Style style, FormattedCharSink sink) {
        ChatItemIconBridge.startModernMarkers();
        return StringDecomposer.iterateFormatted(text, style, babyzombie$wrapped(sink));
    }

    @Inject(method = "createVanillaLayout", at = @At("RETURN"), require = 0)
    private void babyzombie$storeVanilla(CallbackInfoReturnable<Object> cir) {
        finishCollect(cir.getReturnValue());
    }

    // ---- createTextLayout(FormattedText, Style, int, int) ----
    @Redirect(method = "createTextLayout",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/FormattedText;visit" +
                            "(Lnet/minecraft/network/chat/FormattedText$StyledContentConsumer;" +
                            "Lnet/minecraft/network/chat/Style;)Ljava/util/Optional;"),
            require = 0)
    private Optional<Unit> babyzombie$wrapTextVisit(FormattedText text,
                                                    FormattedText.StyledContentConsumer<Unit> original,
                                                    Style style) {
        ChatItemIconBridge.startModernMarkers();
        return text.visit((s, t) ->
                        StringDecomposer.iterateFormatted(t, s, babyzombie$wrapped(mSequenceBuilder))
                                ? Optional.<Unit>empty()
                                : FormattedText.STOP_ITERATION,
                style);
    }

    @Inject(method = "createTextLayout", at = @At("RETURN"), require = 0)
    private void babyzombie$storeText(CallbackInfoReturnable<Object> cir) {
        finishCollect(cir.getReturnValue());
    }

    /** 布局完成：用返回的 TextLayout 的 textBuf 作为 key，把本次收集的标记写入全局表。 */
    private void finishCollect(Object layout) {
        char[] buf = getTextBuf(layout);
        ChatItemIconBridge.storeModernMarkersByText(buf == null ? null : new String(buf));
    }

    private static char[] getTextBuf(Object layout) {
        if (layout == null) return null;
        try {
            if (GET_TEXT_BUF == null) {
                GET_TEXT_BUF = layout.getClass().getMethod("getTextBuf");
                GET_TEXT_BUF.setAccessible(true);
            }
            return (char[]) GET_TEXT_BUF.invoke(layout);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 包装 sink：marker → 全角空格（U+3000，宽 1em = 基础字号 8px，占位与图标等宽）；
     * 可解析出物品时记录 (剥离文本下标, stack)。
     * <p>为什么不用 EM SPACE(U+2003)：现代 UI 字体里 EM SPACE 只有 0.5em(4px)，
     * 图标(8px)会盖过它连同后面的 4px 空格，导致"图标和名字之间没空格"。
     * 全角空格在含思源黑体的现代 UI 字体族里固定 1em = 8px，与图标等宽，
     * 图标恰好覆盖它，名字前的普通空格(4px)保留，视觉为「图标 空格 名字」。
     * <p>下标用 {@link ChatItemIconBridge#bumpModernPos()} 计数器统计（sink 收到的每个码点
     * 即剥离文本的一个字符，{@code StringDecomposer} 负责跳过 § 格式化码），与
     * {@code TextLayout.getTextBuf()} 对齐。解析不到物品的裸标记保持原样（缺字形→空），不污染布局。
     */
    private static FormattedCharSink babyzombie$wrapped(FormattedCharSink original) {
        return (index, style, codePoint) -> {
            if (codePoint == ChatItemIconSupport.MARKER) {
                ItemStack stack = ChatItemIconSupport.stackFromStyle(style);
                if (stack != null) {
                    ChatItemIconBridge.addModernMarker(stack);
                    codePoint = 0x3000; // 全角空格 IDEOGRAPHIC SPACE, 1em = 8px
                }
            }
            if (original.accept(index, style, codePoint)) {
                ChatItemIconBridge.bumpModernPos();
                return true;
            }
            return false;
        };
    }
}