package top.babyzombie.addons.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.Objects;

/** 在 SkyBlock 中强制显示隐身玩家的名称标签（药水效果或服务器隐形标志）。
 *  26.2 起玩家渲染器为 AvatarRenderer，其 shouldShowName 重写为
 *  super(...) && (shouldShowName() || hasCustomName() && 被瞄准)，
 *  基类修改 isVisibleToPlayer 会被外层条件卡死，需直接注入此处。 */
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z",
            at = @At("HEAD"), cancellable = true)
    private void forceShowInvisiblePlayerName(Avatar entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Player)) return;
        if (Objects.equals(entity, Minecraft.getInstance().player)) return;
        if (entity.getName().getString().contains(" ")) return;
        if (!entity.isInvisible()) return;
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
        if (!ModConfigManager.get().skyblock.showInvisibleNameTags) return;
        cir.setReturnValue(true);
    }
}
