package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * 在 SkyBlock 中强制显示因药水效果而隐身的玩家名称标签。
 * 修改 shouldShowName 中的 isVisibleToPlayer 局部变量，
 * 不影响模型渲染（模型仍保持半透明）。
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @ModifyVariable(
            method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
            at = @At("STORE"),
            name = "isVisibleToPlayer")
    private boolean forceVisibleForInvisiblePlayers(boolean isVisibleToPlayer, LivingEntity entity) {
        if (!isVisibleToPlayer
                && entity instanceof Player
                && !entity.getName().getString().contains(" ")
                && entity.hasEffect(MobEffects.INVISIBILITY)
                && HypixelLocationTracker.getInstance().isInSkyblock()
                && ModConfigManager.get().skyblock.showInvisibleNameTags) {
            return true;
        }
        return isVisibleToPlayer;
    }
}
