package top.babyzombie.addons.mixin.item;

import net.minecraft.world.item.ItemInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.config.ModConfigManager;

@Mixin(ItemInstance.class)
public interface ItemStackMaxSizeMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void fixMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        if (!ModConfigManager.get().misc.allowOverMaxStackSize) return;

        int count = ((ItemInstance) this).count();
        if (count > cir.getReturnValue()) {
            cir.setReturnValue(count);
        }
    }
}
