package top.babyzombie.addons.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.babyzombie.addons.config.ModConfigManager;

/**
 * Fix bobber snapping to nearby armor stand entity (ID = hookId + 1).
 * Only active when fishingHookFix config is enabled.
 */
@Mixin(FishingHook.class)
public class FishingHookFixMixin {

    @WrapOperation(method = "onSyncedDataUpdated", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntity(I)Lnet/minecraft/world/entity/Entity;"))
    private Entity fixBooberEntity(Level level, int entityId, Operation<Entity> original) {
        Entity entity = original.call(level, entityId);
        if (!ModConfigManager.get().kuudra.fishingHookFix) return entity;
        FishingHook self = (FishingHook) (Object) this;
        if (entity instanceof ArmorStand && entity.getId() == self.getId() + 1) {
            return null;
        }
        return entity;
    }
}
