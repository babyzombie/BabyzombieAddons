package top.babyzombie.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.PlayerScaleState;

@Mixin(LivingEntityRenderer.class)
public abstract class PlayerScaleMixin {

    @Inject(method = "extractRenderState*", at = @At("HEAD"))
    private void onExtractState(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
        if (entity == Minecraft.getInstance().player) {
            PlayerScaleState.LOCAL_PLAYER_STATES.add(state);
        }
    }
}
