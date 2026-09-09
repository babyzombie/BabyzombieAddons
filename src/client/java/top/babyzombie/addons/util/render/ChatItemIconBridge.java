package top.babyzombie.addons.util.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天物品图标渲染桥（C2 版，节点级提交）。
 *
 * <p>连接"字体渲染阶段"（字形里有坐标、无 GuiRenderer）与"GUI 物品渲染管线"：
 * <ol>
 *   <li>{@code GuiTextRenderState.ensurePrepared} 捕获文本 pose/scissor，字形构造时读取；</li>
 *   <li>{@code GuiRenderer#prepare} HEAD/RETURN 设置/清除当前 GuiRenderer（{@link #currentRenderer}）；</li>
 *   <li>聊天文本打包字形进 {@code GuiRenderState.addGlyphToCurrentLayer} 时，若字形是
 *       我们的物品图标字形，就把 blit 提交到<b>同一节点</b>——与聊天文字同层，
 *       不会被抬到暂停页模糊/其他覆盖层之上。</li>
 * </ol>
 *
 * <p>ModernUI 兼容：ModernUI 用自研排版引擎（TextLayoutProcessor/ModernPreparedText）替换了
 * vanilla 字形管线。{@link #CURRENT_MODERN_MARKERS} 在布局阶段收集"标记字符 → ItemStack"
 * （见 ModernUITextProcessorMixin），ModernPreparedText 构造时读取并生成图标 renderable。
 */
public final class ChatItemIconBridge {

    // ── 文本上下文（ensurePrepared 时捕获，字形构造时读取）──
    private static final ThreadLocal<Matrix3x2f> CURRENT_POSE = new ThreadLocal<>();
    private static final ThreadLocal<ScreenRectangle> CURRENT_SCISSOR = new ThreadLocal<>();

    // ── 渲染阶段持有当前 GuiRenderer（GuiRendererMixin 在 prepare 时设置）──
    private static final ThreadLocal<Object> CURRENT_RENDERER = new ThreadLocal<>();

    // ── ModernUI：布局阶段收集的待渲染图标 ──
    // key = 剥离文本（TextLayout.getTextBuf() 转 String），TextLayout 缓存命中时也能取到。
    // 用「当前布局线程的收集缓冲」配合全局 map：create* 布局完成后写 map，ModernPreparedText
    // 构造时按文本查 map。map 设容量上限防止无界增长。
    private static final ThreadLocal<List<ModernMarker>> CURRENT_MODERN_MARKERS = new ThreadLocal<>();
    private static final ThreadLocal<Integer> MODERN_POS = new ThreadLocal<>();
    private static final Object MODERN_MARK_LOCK = new Object();
    private static final Map<String, List<ModernMarker>> MODERN_MARKERS_BY_TEXT =
            new LinkedHashMap<>(16, 0.75f, true);

    /** ModernUI 布局中收集到的单个图标标记：字符在剥离文本中的下标 + 物品。 */
    public record ModernMarker(int index, ItemStack stack) {}

    private ChatItemIconBridge() {
    }

    // ================================================================
    // 由 mixin 调用
    // ================================================================

    public static void setTextContext(Matrix3x2fc pose, @Nullable ScreenRectangle scissor) {
        CURRENT_POSE.set(new Matrix3x2f(pose));
        CURRENT_SCISSOR.set(scissor);
    }

    public static @Nullable Matrix3x2f currentPose() {
        return CURRENT_POSE.get();
    }

    public static @Nullable ScreenRectangle currentScissor() {
        return CURRENT_SCISSOR.get();
    }

    /**
     * GuiRendererMixin 在 {@code prepare()} HEAD/RETURN 时设置/清除。
     * 值为 GuiRenderer 实例（以 Object 持有，避免依赖关系），
     * 提交逻辑通过 {@code (GuiRendererMixin)(Object)…} 调用其 @Unique 方法。
     */
    public static void setCurrentRenderer(@Nullable Object renderer) {
        CURRENT_RENDERER.set(renderer);
    }

    public static @Nullable Object currentRenderer() {
        return CURRENT_RENDERER.get();
    }

    // ================================================================
    // ModernUI 布局 → 渲染 传递
    // ================================================================

    /** 最大缓存的「文本 → 图标标记」条目数。 */
    private static final int MODERN_MARKERS_CAP = 256;

    /** 开始收集一次 ModernUI 布局的图标标记（布局入口调用，先清空并重置计数器）。 */
    public static void startModernMarkers() {
        CURRENT_MODERN_MARKERS.remove();
        MODERN_POS.set(0);
    }

    /** 记录一个已透传字符（计数器 +1；剥离文本每个字符一个）。 */
    public static void bumpModernPos() {
        MODERN_POS.set(MODERN_POS.get() + 1);
    }

    /** 收集一个图标标记（位置 = 当前计数器）。 */
    public static void addModernMarker(ItemStack stack) {
        List<ModernMarker> list = CURRENT_MODERN_MARKERS.get();
        if (list == null) {
            list = new ArrayList<>(1);
            CURRENT_MODERN_MARKERS.set(list);
        }
        list.add(new ModernMarker(MODERN_POS.get(), stack));
    }

    /**
     * 布局完成后：把本次收集的标记按「剥离文本」写入全局 map（覆盖旧值）。
     * 由 ModernUITextProcessorMixin 在 create*Layout 返回前调用。
     */
    public static void storeModernMarkersByText(String strippedText) {
        List<ModernMarker> list = CURRENT_MODERN_MARKERS.get();
        CURRENT_MODERN_MARKERS.remove();
        if (strippedText == null || list == null || list.isEmpty()) return;
        synchronized (MODERN_MARK_LOCK) {
            MODERN_MARKERS_BY_TEXT.put(strippedText, list);
            // 淘汰最旧条目
            while (MODERN_MARKERS_BY_TEXT.size() > MODERN_MARKERS_CAP) {
                var it = MODERN_MARKERS_BY_TEXT.entrySet().iterator();
                it.next();
                it.remove();
            }
        }
    }

    /**
     * ModernPreparedText 构造时按剥离文本查图标标记。
     *
     * @return 该文本对应的标记列表；无则 null
     */
    public static @Nullable List<ModernMarker> modernMarkersByText(String strippedText) {
        if (strippedText == null) return null;
        synchronized (MODERN_MARK_LOCK) {
            return MODERN_MARKERS_BY_TEXT.get(strippedText);
        }
    }
}