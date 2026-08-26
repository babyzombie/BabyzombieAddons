package top.babyzombie.addons.mixin.render;

import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/** 隐藏 Kuudra 小怪的等级名称标签（名字含 [Lv 的盔甲架）。
 *  26.2 起 ArmorStandRenderer 重写了 shouldShowName 且不调用 super，
 *  基类 LivingEntityRenderer 上的注入对盔甲架不再生效，需直接注入此处。 */
@Mixin(ArmorStandRenderer.class)
public class ArmorStandRendererMixin {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/decoration/ArmorStand;D)Z",
            at = @At("HEAD"), cancellable = true)
    private void hideKuudraMobNametags(ArmorStand entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> cir) {
        if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
        if (!ModConfigManager.get().kuudra.hideMobNametags) return;
        if (ChatUtils.stripColor(entity.getName().getString()).contains("[Lv")) {
            cir.setReturnValue(false);
        }
    }
}
