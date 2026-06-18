package top.babyzombie.addons.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.event.ContainerClickEvents;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerClickMixin {

    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void beforeMouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (ContainerClickEvents.BEFORE_MOUSE_CLICK.invoker()
                .beforeMouseClick((AbstractContainerScreen<?>) (Object) this, hoveredSlot, mouseButtonEvent.button())) {
            cir.setReturnValue(false);
        }
    }
}
