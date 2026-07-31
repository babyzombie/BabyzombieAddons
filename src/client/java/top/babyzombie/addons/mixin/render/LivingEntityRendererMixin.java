package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * 1. 在 SkyBlock 中强制显示因药水效果而隐身的玩家名称标签。
 * 2. 在 Kuudra 中隐藏小怪（[Lvxxx] 盔甲架）的名称标签。
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

    /** 隐藏 Kuudra 小怪的等级名称标签（与 IQ 一致：名字含 [Lv 的盔甲架）。 */
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
            at = @At("HEAD"), cancellable = true)
    private void hideKuudraMobNametags(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof ArmorStand)) return;
        if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
        if (!ModConfigManager.get().kuudra.hideMobNametags) return;
        if (ChatUtils.stripColor(entity.getName().getString()).contains("[Lv")) {
            cir.setReturnValue(false);
        }
    }
}
