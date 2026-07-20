package top.babyzombie.addons.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;
import top.babyzombie.addons.mixin.render.RenderSetupAccessor;
import top.babyzombie.addons.mixin.render.RenderTypeAccessor;

import java.util.List;
import java.util.Map;

import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * SubmitNodeCollector 包装器：拦截 model/modelPart 提交，将 RenderType 替换为半透明变体，
 * 同时将 tintedColor 注入 alpha，实现玩家实体透明度。
 */
public class AlphaSubmitNodeCollector implements SubmitNodeCollector {

    private final OrderedSubmitNodeCollector delegate;
    private final float alpha;

    public AlphaSubmitNodeCollector(OrderedSubmitNodeCollector delegate, float alpha) {
        this.delegate = delegate;
        this.alpha = Math.clamp(alpha, 0.0f, 1.0f);
    }

    private int applyAlpha(int color) {
        if (alpha >= 1.0f) return color;
        int a = (int) (alpha * 255.0f);
        return (color & 0x00FFFFFF) | (a << 24);
    }

    /** 尝试将 RenderType 转换为带混合的半透明变体，失败则返回原始类型 */
    private RenderType tryMakeTranslucent(RenderType rt) {
        if (alpha >= 1.0f || rt.hasBlending()) return rt;
        try {
            Identifier tex = extractTexture(rt);
            if (tex != null) {
                return RenderTypes.entityTranslucent(tex);
            }
        } catch (Exception ignored) {
        }
        return rt;
    }

    /** 从 RenderType 中提取纹理 Identifier（通过 Accessor + 反射） */
    @Nullable
    private static Identifier extractTexture(RenderType rt) {
        RenderSetup state = ((RenderTypeAccessor) rt).getState();
        Map<String, Object> bindings = ((RenderSetupAccessor) (Object) state).getTextureBindings();
        if (bindings.isEmpty()) return null;
        Object binding = bindings.values().iterator().next();
        try {
            return (Identifier) binding.getClass().getMethod("location").invoke(binding);
        } catch (Exception e) {
            return null;
        }
    }

    // ===== submitModel =====

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                                int lightCoords, int overlayCoords, int tintedColor,
                                @Nullable TextureAtlasSprite sprite, int outlineColor,
                                ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        delegate.submitModel(model, state, poseStack, tryMakeTranslucent(renderType), lightCoords, overlayCoords,
                applyAlpha(tintedColor), sprite, outlineColor, crumblingOverlay);
    }

    // ===== submitModelPart =====

    @Override
    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType,
                                int lightCoords, int overlayCoords, @Nullable TextureAtlasSprite sprite,
                                int tintedColor,
                                ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay, int outlineColor) {
        delegate.submitModelPart(modelPart, poseStack, tryMakeTranslucent(renderType), lightCoords, overlayCoords,
                sprite, applyAlpha(tintedColor), crumblingOverlay, outlineColor);
    }

    // ===== submitItem =====

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords,
                           int overlayCoords, int outlineColor, int[] tintLayers,
                           List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
        delegate.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor,
                tintLayers, quads, foilType);
    }

    // ===== 直接委托 =====

    @Override public OrderedSubmitNodeCollector order(int order) { return delegate; }
    @Override public void submitShadow(PoseStack p, float r, List<EntityRenderState.ShadowPiece> l) { delegate.submitShadow(p, r, l); }
    @Override public void submitNameTag(PoseStack p, @Nullable Vec3 v, int o, Component n, boolean s, int l, CameraRenderState c) { delegate.submitNameTag(p, v, o, n, s, l, c); }
    @Override public void submitText(PoseStack p, float x, float y, FormattedCharSequence s, boolean d, Font.DisplayMode m, int l, int c, int bg, int ol) { delegate.submitText(p, x, y, s, d, m, l, c, bg, ol); }
    @Override public void submitFlame(PoseStack p, EntityRenderState r, Quaternionf q) { delegate.submitFlame(p, r, q); }
    @Override public void submitLeash(PoseStack p, EntityRenderState.LeashState l) { delegate.submitLeash(p, l); }
    @Override public void submitMovingBlock(PoseStack p, MovingBlockRenderState m, int outlineColor) { delegate.submitMovingBlock(p, m, outlineColor); }
    @Override public void submitBlockModel(PoseStack p, RenderType r, List<BlockStateModelPart> parts, int[] t, int l, int o, int ol) { delegate.submitBlockModel(p, r, parts, t, l, o, ol); }
    @Override public void submitBreakingBlockModel(PoseStack p, List<BlockStateModelPart> parts, int pr) { delegate.submitBreakingBlockModel(p, parts, pr); }
    @Override public void submitCustomGeometry(PoseStack p, RenderType r, SubmitNodeCollector.CustomGeometryRenderer g) { delegate.submitCustomGeometry(p, r, g); }
    @Override public void submitQuadParticleGroup(QuadParticleRenderState particles) { delegate.submitQuadParticleGroup(particles); }
    @Override public void submitShapeOutline(PoseStack p, VoxelShape s, RenderType r, int c, float w, boolean a) { delegate.submitShapeOutline(p, s, r, c, w, a); }
    @Override public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group g, CameraRenderState c, boolean o) { delegate.submitGizmoPrimitives(g, c, o); }
}
