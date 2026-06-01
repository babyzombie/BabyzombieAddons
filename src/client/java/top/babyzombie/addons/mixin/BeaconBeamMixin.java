package top.babyzombie.addons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.BeaconBeamRenderer;

@Mixin(LevelRenderer.class)
public class BeaconBeamMixin {
    @Inject(method = "submitBlockEntities", at = @At("TAIL"))
    private void afterSubmitBlockEntities(PoseStack poseStack, LevelRenderState rs,
            SubmitNodeStorage submitNodeStorage, CallbackInfo ci) {
        Vec3 cam = rs.cameraRenderState.pos;
        BeaconBeamRenderer.renderWorldBeams(submitNodeStorage, cam.x, cam.y, cam.z);
    }
}
