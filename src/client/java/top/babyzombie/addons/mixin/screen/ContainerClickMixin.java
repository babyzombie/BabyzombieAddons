package top.babyzombie.addons.mixin.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.module.chat.containerchat.ItemProtectBridge;
import top.babyzombie.addons.module.kuudra.ChestCounter;
import top.babyzombie.addons.module.misc.CopyItemInfoKey;
import top.babyzombie.addons.module.misc.pet.PetPageKeyHandler;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.gui.overlay.GuiOverlayManager;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerClickMixin {

    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "extractRenderState*", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        ChestCounter.renderOnScreen(graphics, mouseX, mouseY);
        GuiOverlayManager.onRender((AbstractContainerScreen<?>) (Object) this, graphics, mouseX, mouseY, a);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void beforeMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        // [根因修复 BZ价格点击] ScreenMouseEvents.allowMouseClick 注入到 Screen.mouseClicked，
        // 但 AbstractContainerScreen 重写了该方法且不调用 super，导致 Fabric API 的
        // allowMouseClick 在容器界面上完全不触发。因此 GuiOverlayManager（BZ overlay 等）
        // 的鼠标点击必须在此 Mixin 中直接调用，否则 BZ 页面价格点击无法到达 overlay。
        // 不会有双重触发：Fabric allowMouseClick 在 AbstractContainerScreen 上不触发。
        if (GuiOverlayManager.onMouseClicked((AbstractContainerScreen<?>) (Object) this, event.x(), event.y(), event.button())) {
            cir.setReturnValue(false);
            return;
        }
        if (ChestCounter.onScreenClick(event)) {
            cir.setReturnValue(false);
            return;
        }
        // 宠物页面鼠标按键：用户把按键绑到鼠标（侧键等）时 keyPressed 收不到鼠标事件，
        // 必须在点击入口同样匹配；命中后取消本次点击（键码 0-7 = 鼠标按键，
        // 与键盘键码无交集，键盘绑定不会误触发）。
        if (PetPageKeyHandler.handleKeyPress(event.button())) {
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
                ItemStack stack = hoveredSlot.getItem();
                String text = ItemUtils.formatItemCopyText(stack);
                Minecraft.getInstance().keyboardHandler.setClipboard(text);
                ChatUtils.showToast(stack,
                        Component.translatable("config.babyzombieaddons.copyitem.toast.copiedTitle"),
                        stack.getHoverName());
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
