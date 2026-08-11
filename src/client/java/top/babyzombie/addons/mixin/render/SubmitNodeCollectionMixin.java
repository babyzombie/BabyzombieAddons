package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
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
import top.babyzombie.addons.util.render.SecondCameraRenderer;

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
        // 第二相机捕获期间不提交深度发光 outline:第二遍渲染的 outline 画到
        // 共享 entityOutlineTarget,主画面 doEntityOutline 输出会被污染(发光闪烁/出框)
        if (SecondCameraRenderer.capturing) return;
        EntityRenderState ers = CurrentEntityTracker.STATE.get();
        if (ers == null || !ers.getDataOrDefault(GlowController.NEEDS_DEPTH_TEST, false)) return;
        RenderType dt = renderType.isOutline() ? renderType
            : ((GlowRenderTypeHolder)(Object)renderType).babyzombie$getGlowRenderType().orElse(null);
        if (dt == null) return;
        this.outline.submit(new ModelFeatureRenderer.Submit<>(dt, pose, model, state,
            LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
            ers.getData(GlowController.DEPTH_GLOW_COLOR), sprite, null));
    }

    @ModifyArg(method = "submitItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"),
        slice = @Slice(from = @At(value = "FIELD",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;outline:Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;",
            opcode = Opcodes.GETFIELD)))
    private SubmitNode markItem(SubmitNode submit) {
        if (SecondCameraRenderer.capturing) return submit;
        EntityRenderState ers = CurrentEntityTracker.STATE.get();
        if (ers != null && ers.getDataOrDefault(GlowController.NEEDS_DEPTH_TEST, false))
            ((DepthTestMarker)(Object)submit).babyzombie$setNeedsDepthTest(true);
        return submit;
    }
}
