package top.babyzombie.addons.mixin.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.module.chat.ItemProtectBridge;
import top.babyzombie.addons.module.misc.CopyItemInfoKey;
import top.babyzombie.addons.util.ItemUtils;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerClickMixin {

    @Shadow
    protected Slot hoveredSlot;

    // ALT+左键 物品分享/收藏
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void beforeMouseClicked(MouseButtonEvent event, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (ContainerClickEvents.BEFORE_MOUSE_CLICK.invoker()
                .beforeMouseClick((AbstractContainerScreen<?>) (Object) this, hoveredSlot, event)) {
            cir.setReturnValue(false);
        }
    }

    // 收藏物品防丢弃（仅在自维护后端起效，GUI 内全域生效）
    @Inject(method = "slotClicked", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleContainerInput(IIILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V"),
            cancellable = true)
    private void protectCollectedItem(Slot slot, int slotId, int button, ContainerInput input, CallbackInfo ci) {
        if (slot == null || !slot.hasItem()) return;
        if (input != ContainerInput.THROW) return;
        if (!ItemProtectBridge.needsOwnProtection()) return;
        if (!ItemProtectBridge.isProtected(slot.getItem())) return;

        ci.cancel();
    }

    // 复制物品信息按键
    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (CopyItemInfoKey.KEY.matches(event)) {
            if (hoveredSlot != null && hoveredSlot.hasItem()) {
                String text = ItemUtils.formatItemCopyText(hoveredSlot.getItem());
                Minecraft.getInstance().keyboardHandler.setClipboard(text);
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.5f, 2.0f);
                }
            }
        }
    }
}
