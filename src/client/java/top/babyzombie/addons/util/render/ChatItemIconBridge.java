package top.babyzombie.addons.util.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天物品图标 C2 渲染桥：
 *
 * <p>把"字体渲染阶段"（字形里有坐标、无 GuiRenderer）与"GUI 物品渲染管线"
 * （GuiRenderer 每帧的 prepare 阶段有 item-atlas 烘焙能力）连接起来。
 *
 * <p>数据流：
 * <ol>
 *   <li>字形存活期间，{@link ChatItemIconRenderable#render} 每帧把
 *       (ItemStack, pose, scissor, x, y) 压进 {@link #PENDING}；</li>
 *   <li>{@code GuiRenderer.prepare()} 在 prepareText 之后排空 {@link #PENDING}，
 *       用 ItemModelResolver + GuiItemAtlas 烘出官方物品图标并以 blit 提交到聊天图层
 *       （隔一帧生效，肉眼不可见）；</li>
 *   <li>文本被移除后字形不再渲染 → 不再产生请求 → 图标自然消失。</li>
 * </ol>
 *
 * <p>文本 pose/scissor 由 {@code GuiTextRenderState.ensurePrepared} 的 mixin 捕获，
 * 供字形构造时读取。
 */
public final class ChatItemIconBridge {

    // ── 文本上下文（ensurePrepared 时捕获，字形构造时读取）──
    private static final ThreadLocal<Matrix3x2f> CURRENT_POSE = new ThreadLocal<>();
    private static final ThreadLocal<ScreenRectangle> CURRENT_SCISSOR = new ThreadLocal<>();

    // ── 每帧待烘请求 ──
    private static final List<ChatIconRequest> PENDING = new ArrayList<>();
    private static final int MAX_PENDING = 256;

    /**
     * 一次聊天图标渲染请求：完整 ItemStack + 字形处的局部坐标 + 文本 pose/scissor。
     */
    public record ChatIconRequest(ItemStack stack, Matrix3x2f pose,
                                  @Nullable ScreenRectangle scissor, float x, float y) {
    }

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

    // ================================================================
    // 字形每帧请求 + GuiRenderer 排空
    // ================================================================

    /** 字形 render() 每帧调用：登记一个待烘请求。 */
    public static void request(ItemStack stack, Matrix3x2f pose,
                               @Nullable ScreenRectangle scissor, float x, float y) {
        if (stack == null || stack.isEmpty() || pose == null) return;
        // 兜底：防止桥未排空（异常路径）时无限积累
        if (PENDING.size() >= MAX_PENDING) {
            PENDING.clear();
        }
        PENDING.add(new ChatIconRequest(stack, pose, scissor, x, y));
    }

    /** GuiRenderer.prepare() 每帧排空；返回本次待烘请求列表（copy，随后清空）。 */
    public static List<ChatIconRequest> drain() {
        if (PENDING.isEmpty()) return List.of();
        List<ChatIconRequest> out = new ArrayList<>(PENDING);
        PENDING.clear();
        return out;
    }
}