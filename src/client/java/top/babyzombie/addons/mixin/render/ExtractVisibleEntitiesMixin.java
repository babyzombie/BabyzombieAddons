package top.babyzombie.addons.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.fishing.FishingCameraModule;

/// 第二相机捕获期间,把玩家实体追加进提取列表。
/// 原版规则:LocalPlayer 只在相机实体 == 玩家时渲染(第一人称看不到自己);
/// 第二相机的相机实体是虚拟 Marker,玩家实体会被剔除,这里手动补上。
@Mixin(LevelRenderer.class)
public class ExtractVisibleEntitiesMixin {

    @Inject(method = "extractVisibleEntities", at = @At("RETURN"))
    private void babyzombieaddons$extractPlayer(Camera camera, Frustum frustum, DeltaTracker deltaTracker,
                                                LevelRenderState output, CallbackInfo ci) {
        if (!FishingCameraModule.capturing) return;
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;
        var dispatcher = ((LevelRendererAccessor) this).getEntityRenderDispatcher();
        Vec3 camPos = camera.position();
        if (!dispatcher.shouldRender(player, frustum, camPos.x, camPos.y, camPos.z)) return;
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        output.entityRenderStates.add(((LevelRendererInvoker) this).invokeExtractEntity(player, partialTick));
    }
}
