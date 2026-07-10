package top.babyzombie.addons.mixin.screen;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.event.ContainerClickEvents;

/**
 * 在 handleContainerInput（所有容器操作的最终公共路径）触发事件。
 * 不管是真实鼠标点击还是 IQ 等模组的程序化调用都会经过这里。
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class HandleContainerInputMixin {

    @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
    private void beforeHandleContainerInput(int containerId, int slotId, int buttonNum,
                                             ContainerInput input, Player player,
                                             CallbackInfo ci) {
        if (ContainerClickEvents.BEFORE_CONTAINER_INPUT.invoker()
                .beforeContainerInput(player, containerId, slotId, buttonNum, input)) {
            ci.cancel();
        }
    }
}
