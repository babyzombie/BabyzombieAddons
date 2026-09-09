package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/**
 * 聊天物品图标字形（C2 版）。
 *
 * <p>它占用聊天文本里一个字形槽位（保证布局、悬停、生命周期都正确），但自身不画任何东西：
 * 打包字形进 {@code GuiRenderState.addGlyphToCurrentLayer} 时，由 GuiRenderStateMixin
 * 用官方物品渲染管线（ItemModelResolver + GuiItemAtlas）烘出完整物品图标
 * （支持头颅、自定义模型、染色、附魔光泽）并以 blit 形式提交到<b>聊天同一节点</b>。
 *
 * <p>{@link #guiPipeline()}/{@link #textureView()} 始终返回非空值，因为字形会被
 * {@code GuiRenderer.prepareText} 包成 GlyphRenderState 并调用这两者；绘制本身无操作。
 */
public final class ChatItemIconRenderable implements TextRenderable.Styled {

    /**
     * 图标显示/布局尺寸（8px）。
     * 注意：物品图集烘烤仍用 16px 槽位（见 GuiRendererMixin），此处只决定 blit 四边形大小。
     */
    public static final int ICON_SIZE = 8;

    private final float x;
    private final float y;
    private final ItemStack stack;
    private final Matrix3x2f pose;
    private final @Nullable ScreenRectangle scissor;
    private final Style style;
    private final GpuTextureView atlasTextureView;

    public ChatItemIconRenderable(float x, float y, ItemStack stack,
                                  Matrix3x2f pose, @Nullable ScreenRectangle scissor, Style style) {
        this.x = x;
        this.y = y;
        this.stack = stack;
        this.pose = pose;
        this.scissor = scissor;
        this.style = style;
        this.atlasTextureView = resolveAtlasTextureView();
    }

    /** 每帧调用（文本存活期间）。图标 blit 在打包字形进节点时提交（见 GuiRenderStateMixin），此处无操作。 */
    @Override
    public void render(Matrix4fc poseMatrix, VertexConsumer buffer, int packedLightCoords, boolean flat) {
    }

    public ItemStack stack() { return stack; }

    public Matrix3x2f pose() { return pose; }

    public @Nullable ScreenRectangle scissor() { return scissor; }

    public float x() { return x; }

    public float y() { return y; }

    @Override
    public RenderType renderType(Font.DisplayMode displayMode) {
        return RenderTypes.text(ChatItemIconSupport.itemAtlasLocation());
    }

    /** 新管线会用 textureView 构建 TextureSetup；始终返回 item atlas 纹理视图（非空）。 */
    @Override
    public GpuTextureView textureView() {
        return atlasTextureView;
    }

    /** 新管线会用 guiPipeline 构建 GlyphRenderState；返回 GUI 文本管线（普通字形同款）。 */
    @Override
    public RenderPipeline guiPipeline() {
        return RenderPipelines.GUI_TEXT;
    }

    @Override
    public float left() {
        return x;
    }

    @Override
    public float top() {
        return y;
    }

    @Override
    public float right() {
        return x + ICON_SIZE;
    }

    @Override
    public float bottom() {
        return y + ICON_SIZE;
    }

    @Override
    public Style style() {
        return style;
    }

    /**
     * item atlas 纹理视图。游戏加载后图集必然注册，理论不会失败；
     * 若极端情况下取不到，缺失纹理也会返回非空的纹理视图，因此结果恒非空。
     */
    private static GpuTextureView resolveAtlasTextureView() {
        return Minecraft.getInstance().getTextureManager()
                .getTexture(ChatItemIconSupport.itemAtlasLocation())
                .getTextureView();
    }
}