package top.babyzombie.addons.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.chat.ContainerChatHelper;

/**
 * InventoryScreen 覆写了 extractRenderState 且不调 super，
 * 导致 AbstractContainerScreen 的 overlay 注入不触发。
 */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor g, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (ContainerChatHelper.isActive()) {
            ContainerChatHelper.getOverlay().extractRenderState(g, mouseX, mouseY, a);
        }
    }
}
