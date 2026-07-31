package top.babyzombie.addons.mixin.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import top.babyzombie.addons.module.kuudra.ChestCounter;
import top.babyzombie.addons.module.misc.CopyItemInfoKey;
import top.babyzombie.addons.module.misc.pet.PetPageKeyHandler;
import top.babyzombie.addons.util.ItemUtils;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerClickMixin {

    @Shadow
    protected Slot hoveredSlot;

    // 容器页面渲染箱子计数 HUD（hover 提示）
    @Inject(method = "extractRenderState*", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        ChestCounter.renderOnScreen(graphics, mouseX, mouseY);
    }

    // 点击箱子计数 HUD（容器/背包页面）→ 触发指令；ALT+左键 物品分享/收藏
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void beforeMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (ChestCounter.onScreenClick(event)) {
            cir.setReturnValue(false);
            return;
        }
        if (ContainerClickEvents.BEFORE_MOUSE_CLICK.invoker()
                .beforeMouseClick((AbstractContainerScreen<?>) (Object) this, hoveredSlot, event)) {
            cir.setReturnValue(false);
        }
    }

    // 收藏物品防丢弃（仅在自维护后端起效，GUI 内全域生效）
    @Inject(method = "slotClicked", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleContainerInput(IIILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V"),
            cancellable = true)
    private void protectCollectedItem(Slot slot, int slotId, int buttonNum, ContainerInput containerInput, CallbackInfo ci) {
        if (slot == null || !slot.hasItem()) return;
        if (containerInput != ContainerInput.THROW) return;
        if (!ItemProtectBridge.needsOwnProtection()) return;
        if (!ItemProtectBridge.isProtected(slot.getItem())) return;

        ci.cancel();
    }

    // 复制物品信息按键
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
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
        // 宠物页面按键
        if (PetPageKeyHandler.handleKeyPress(event.key())) {
            cir.setReturnValue(true);
        }
    }
}
