package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.CurrentEntityTracker;
import top.babyzombie.addons.util.render.DepthTestMarker;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.render.GlowRenderTypeHolder;

import java.util.List;
import java.util.Set;

/**
 * 深度测试发光：实体模型提交时额外提交深度 outline 节点（被墙挡的发光描边）。
 */
@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin {
    @Shadow @Final public SimpleFeatureRenderPhase outline;

    @Inject(method = "submitModel", at = @At("RETURN"))
    private <S> void submitDepthOutline(CallbackInfo ci,
            @Local(name = "model") Model<? super S> model,
            @Local(name = "state") S state,
            @Local(name = "renderType") RenderType renderType,
            @Local(name = "sprite") TextureAtlasSprite sprite,
            @Local(name = "pose") PoseStack.Pose pose) {
        EntityRenderState ers = CurrentEntityTracker.STATE.get();
        if (ers == null || !ers.getDataOrDefault(GlowController.NEEDS_DEPTH_TEST, false)) return;
        // 选择性发光时，已由原版 outline 分支 + markModel 换成深度 RenderType，这里不再额外补
        if (isSelective(ers)) return;
        RenderType dt = renderType.isOutline() ? renderType
            : ((GlowRenderTypeHolder)(Object)renderType).babyzombie$getGlowRenderType().orElse(null);
        if (dt == null) return;
        this.outline.submit(new ModelFeatureRenderer.Submit<>(dt, pose, model, state,
            LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
            ers.getData(GlowController.DEPTH_GLOW_COLOR), sprite, null));
    }

    @Inject(method = "submitItem", at = @At("HEAD"))
    private void submitItemDepthOutline(CallbackInfo ci,
            @Local(argsOnly = true) PoseStack poseStack,
            @Local(argsOnly = true) ItemDisplayContext displayContext,
            @Local(argsOnly = true, ordinal = 2) int outlineColor,
            @Local(argsOnly = true) int[] tints,
            @Local(argsOnly = true) List<BakedQuad> quads,
            @Local(argsOnly = true) ItemStackRenderState.FoilType foilType) {
        // submitItem 只在 outlineColor != 0 时才会提交 outline；深度测试发光不设 outlineColor，
        // 所以普通物品（无 SpecialModelRenderer）需要在这里补一个深度 outline。
        if (outlineColor != 0) return;
        EntityRenderState ers = CurrentEntityTracker.STATE.get();
        if (ers == null || !ers.getDataOrDefault(GlowController.NEEDS_DEPTH_TEST, false)) return;
        // 选择性发光由 layer mixin 设置 outlineColor 并走原版 outline 提交，这里不再额外补
        if (isSelective(ers)) return;
        ItemFeatureRenderer.Submit submit = new ItemFeatureRenderer.Submit(
                poseStack.last().copy(), displayContext,
                LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                ers.getData(GlowController.DEPTH_GLOW_COLOR),
                ItemStackRenderState.LayerRenderState.EMPTY_TINTS, quads,
                ItemStackRenderState.FoilType.NONE);
        ((DepthTestMarker)(Object) submit).babyzombie$setNeedsDepthTest(true);
        this.outline.submit(submit);
    }

    @ModifyArg(method = "submitModel", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"),
        slice = @Slice(from = @At(value = "FIELD",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;outline:Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;",
            opcode = Opcodes.GETFIELD)))
    private SubmitNode markModel(SubmitNode submit,
            @Local(name = "renderType") RenderType renderType) {
        EntityRenderState ers = CurrentEntityTracker.STATE.get();
        if (ers == null || !ers.getDataOrDefault(GlowController.NEEDS_DEPTH_TEST, false)
                || !isSelective(ers) || ers.outlineColor == 0) {
            return submit;
        }
        if (submit instanceof ModelFeatureRenderer.Submit<?> ms) {
            RenderType dt = renderType.isOutline() ? renderType
                : ((GlowRenderTypeHolder)(Object) renderType).babyzombie$getGlowRenderType().orElse(null);
            if (dt != null) {
                return new ModelFeatureRenderer.Submit(dt, ms.pose(), ms.model(), ms.state(),
                        ms.lightCoords(), ms.overlayCoords(), ms.tintedColor(), ms.sprite(), ms.sheetedDecalPose());
            }
        }
        return submit;
    }

    @ModifyArg(method = "submitItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"),
        slice = @Slice(from = @At(value = "FIELD",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;outline:Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;",
            opcode = Opcodes.GETFIELD)))
    private SubmitNode markItem(SubmitNode submit) {
        EntityRenderState ers = CurrentEntityTracker.STATE.get();
        if (ers != null
                && ers.getDataOrDefault(GlowController.NEEDS_DEPTH_TEST, false)
                && (!isSelective(ers) || ers.outlineColor != 0))
            ((DepthTestMarker)(Object)submit).babyzombie$setNeedsDepthTest(true);
        return submit;
    }

    private static boolean isSelective(EntityRenderState ers) {
        return ers != null && !ers.getDataOrDefault(GlowController.SELECTIVE_SLOTS, Set.of()).isEmpty();
    }
}
